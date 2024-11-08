package org.jahia.modules.errorpages.handler;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.errors.ErrorHandler;
import org.jahia.exceptions.*;
import org.jahia.modules.errorpages.ErrorPageRequestWrapper;
import org.jahia.modules.errorpages.service.ErrorPageService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.*;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * capture rendering exceptions and redirect them to custom error pages 400, 401, 403, 404, 500
 *
 * @author BEN AJIBA Youssef
 */
public class ErrorPageHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorPageHandler.class);
    private URLResolverFactory urlResolverFactory;
    private ErrorPageService errorPageService;
    private JahiaSitesService siteService;

    @Override
    public boolean handle(Throwable e, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        final ErrorPageRequestWrapper requestWrapper = new ErrorPageRequestWrapper(request);
        if (e instanceof PathNotFoundException) {
            code = HttpServletResponse.SC_NOT_FOUND;
        } else if (e instanceof TemplateNotFoundException) {
            code = HttpServletResponse.SC_NOT_FOUND;
        } else if (e instanceof AccessDeniedException) {
            if (JahiaUserManagerService.isGuest(JCRSessionFactory.getInstance().getCurrentUser())) {
                code = HttpServletResponse.SC_UNAUTHORIZED;
            } else {
                code = HttpServletResponse.SC_FORBIDDEN;
            }
        } else if (e instanceof JahiaRuntimeException) {
            if (e instanceof JahiaBadRequestException) {
                code = HttpServletResponse.SC_BAD_REQUEST;
            } else if (e instanceof JahiaUnauthorizedException) {
                code = HttpServletResponse.SC_UNAUTHORIZED;
            } else if (e instanceof JahiaNotFoundException) {
                code = HttpServletResponse.SC_NOT_FOUND;
            }
        } else if (e instanceof ClassNotFoundException) {
            code = HttpServletResponse.SC_BAD_REQUEST;
        }

        URLResolver urlResolver = (URLResolver) requestWrapper.getAttribute("urlResolver");
        if (urlResolver == null) {
            urlResolver = urlResolverFactory.createURLResolver(requestWrapper.getPathInfo(), requestWrapper.getServerName(), requestWrapper);
        }
        if (Constants.LIVE_WORKSPACE.equals(urlResolver.getWorkspace())) {
            String sitePath = null;
            if (urlResolver.getSiteInfo() != null) {
                sitePath = urlResolver.getSiteInfo().getSitePath();
            } else {
                sitePath = siteService.getDefaultSite().getJCRLocalPath();
            }
            if (StringUtils.isNotBlank(sitePath)) {
                boolean system = (code == HttpServletResponse.SC_UNAUTHORIZED || code == HttpServletResponse.SC_FORBIDDEN);
                // render page with root session
                return render(requestWrapper, response, urlResolver, sitePath, code, system);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error(String.format("Site not found for URL %s?%s", requestWrapper.getRequestURL().toString(), requestWrapper.getQueryString()));
                } else {
                    LOGGER.error(String.format("Site not found for URI %s", requestWrapper.getRequestURI()));
                }
            }
        }
        return false;
    }

    public boolean render(final HttpServletRequest request, final HttpServletResponse response, final URLResolver urlResolver, final String sitePath, final int errorStatus, boolean system) {
        try {
            Locale locale = urlResolver.getLocale();
            Iterator requestLocale = request.getLocales().asIterator();
            JahiaSite siteByKey = siteService.getSiteByKey(StringUtils.substringAfterLast(sitePath, "/"));
            List<Locale> languagesAsLocales = siteByKey.getLanguagesAsLocales();
            while (requestLocale.hasNext()) {
                Locale next = (Locale) requestLocale.next();
                if (languagesAsLocales.contains(next)) {
                    locale = next;
                    break;
                }
            }
            if (system) {
                // render page with root session
                return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(errorPageService.lookupRootUser().getJahiaUser(), urlResolver.getWorkspace(), locale, (JCRSessionWrapper session) -> render(session, request, response, urlResolver, sitePath, errorStatus));
            } else {
                // render page with current user session
                JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(urlResolver.getWorkspace(), locale, null);
                return render(session, request, response, urlResolver, sitePath, errorStatus);
            }
        } catch (RepositoryException | JahiaException e) {
            LOGGER.error(String.format("Error while rendering an error page for the code %d", errorStatus), e);
        }
        return false;
    }

    public boolean render(JCRSessionWrapper session, HttpServletRequest request, HttpServletResponse response, URLResolver urlResolver, String sitePath, int errorStatus) {
        try {
            JCRNodeWrapper node = errorPageService.getSettingsNode(session, sitePath);
            if (node != null) {
                String errorStatusString = String.valueOf(errorStatus);
                if (node.hasNode(errorStatusString)) {
                    node = node.getNode(errorStatusString);
                    if (node.hasProperty(ErrorPageService.ERROR_PAGE_TARGET)) {
                        node = (JCRNodeWrapper) node.getProperty(ErrorPageService.ERROR_PAGE_TARGET).getNode();
                        JCRSiteNode site = (JCRSiteNode) session.getNode(sitePath);

                        RenderContext renderContext = errorPageService.createRenderContext(request, response, session.getUser());
                        renderContext.setWorkspace(urlResolver.getWorkspace());
                        renderContext.setSiteInfo(urlResolver.getSiteInfo());
                        renderContext.setSite(site);

                        Resource resource = new Resource(node, "html", null, Resource.CONFIGURATION_PAGE);
                        renderContext.setMainResource(resource);

                        String out = RenderService.getInstance().render(resource, renderContext).trim();
                        response.setHeader("Content-Type", "text/html");
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.setStatus(errorStatus);
                        response.getOutputStream().write(out.getBytes(StandardCharsets.UTF_8));
                        return true;
                    }
                }
            }
        } catch (ItemNotFoundException e) {
            LOGGER.error(String.format("The node %s of error %d does not exist or not published or not authorized to current user", e.getMessage(), errorStatus));
        } catch (RepositoryException | RenderException | IOException e) {
            LOGGER.error(String.format("Error while rendering an error page for the code %d", errorStatus), e);
        }
        return false;
    }

    public void setErrorPageService(ErrorPageService errorPageService) {
        this.errorPageService = errorPageService;
    }

    public void setUrlResolverFactory(URLResolverFactory urlResolverFactory) {
        this.urlResolverFactory = urlResolverFactory;
    }

    public void setSiteService(JahiaSitesService siteService) {
        this.siteService = siteService;
    }
}
