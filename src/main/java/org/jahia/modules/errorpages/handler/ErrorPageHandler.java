package org.jahia.modules.errorpages.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.errors.ErrorHandler;
import org.jahia.exceptions.JahiaBadRequestException;
import org.jahia.exceptions.JahiaNotFoundException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.exceptions.JahiaUnauthorizedException;
import org.jahia.modules.errorpages.ErrorPageRequestWrapper;
import org.jahia.modules.errorpages.service.ErrorPageService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.RenderException;
import org.jahia.services.render.RenderService;
import org.jahia.services.render.Resource;
import org.jahia.services.render.TemplateNotFoundException;
import org.jahia.services.render.URLResolver;
import org.jahia.services.render.URLResolverFactory;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;

/**
 * capture rendering exceptions and redirect them to custom error pages 400, 401, 403, 404, 500
 *
 * @author BEN AJIBA Youssef
 */
public class ErrorPageHandler implements ErrorHandler {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ErrorPageHandler.class);
    private URLResolverFactory urlResolverFactory;
    private ErrorPageService errorPageService;

    @Override
    public boolean handle(Throwable e, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        final ErrorPageRequestWrapper requestWrapper = new ErrorPageRequestWrapper(request);
        if (e instanceof PathNotFoundException) {
            code = HttpStatus.SC_NOT_FOUND;
        } else if (e instanceof TemplateNotFoundException) {
            code = HttpStatus.SC_NOT_FOUND;
        } else if (e instanceof AccessDeniedException) {
            if (JahiaUserManagerService.isGuest(JCRSessionFactory.getInstance().getCurrentUser())) {
                code = HttpStatus.SC_UNAUTHORIZED;
            } else {
                code = HttpStatus.SC_FORBIDDEN;
            }
        } else if (e instanceof JahiaRuntimeException) {
            if (e instanceof JahiaBadRequestException) {
                code = HttpStatus.SC_BAD_REQUEST;
            } else if (e instanceof JahiaUnauthorizedException) {
                code = HttpStatus.SC_UNAUTHORIZED;
            } else if (e instanceof JahiaNotFoundException) {
                code = HttpStatus.SC_NOT_FOUND;
            }
        } else if (e instanceof ClassNotFoundException) {
            code = HttpStatus.SC_BAD_REQUEST;
        }

        URLResolver urlResolver = (URLResolver) requestWrapper.getAttribute("urlResolver");
        if (urlResolver == null) {
            urlResolver = urlResolverFactory.createURLResolver(requestWrapper.getPathInfo(), requestWrapper.getServerName(), requestWrapper);
        }
        if (Constants.LIVE_WORKSPACE.equals(urlResolver.getWorkspace())) {
            String sitePath = null;
            if (urlResolver.getSiteInfo() != null) {
                sitePath = urlResolver.getSiteInfo().getSitePath();
            }
            if (StringUtils.isNotBlank(sitePath)) {
                boolean system = false;
                if (code == HttpStatus.SC_UNAUTHORIZED || code == HttpStatus.SC_FORBIDDEN) {
                    // render page with root session
                    system = true;
                }
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
            if (system) {
                // render page with root session
                return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(errorPageService.lookupRootUser().getJahiaUser(), urlResolver.getWorkspace(), urlResolver.getLocale(), (JCRSessionWrapper session) -> render(session, request, response, urlResolver, sitePath, errorStatus));
            } else {
                // render page with current user session
                JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(urlResolver.getWorkspace(), urlResolver.getLocale(), null);
                return render(session, request, response, urlResolver, sitePath, errorStatus);
            }
        } catch (RepositoryException e) {
            LOGGER.error(String.format("Error while rendering an error page for the code %d", errorStatus), e);
        }
        return false;
    }

    public boolean render(JCRSessionWrapper session, HttpServletRequest request, HttpServletResponse response, URLResolver urlResolver, String sitePath, int errorStatus) {
        try {
            JCRNodeWrapper node = errorPageService.getSettingsNode(session, sitePath);
            if (node != null && node.hasNode(String.valueOf(errorStatus))) {
                node = node.getNode(String.valueOf(errorStatus));
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
}
