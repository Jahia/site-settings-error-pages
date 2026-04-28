package org.jahia.modules.errorpages.webflow;

import java.util.List;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import org.jahia.api.Constants;
import org.jahia.modules.errorpages.service.ErrorPageService;
import org.jahia.modules.errorpages.service.ErrorPageServiceImpl;
import org.jahia.modules.errorpages.webflow.bean.ErrorPageSettings;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRNodeWrapperImpl;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

@Component(service = RenderFilter.class)
public class ErrorPageFilter extends AbstractFilter {

  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ErrorPageFilter.class);
  private static final String ERROR_PAGE_SETTINGS_PARAM = "errorPageSettings";

  @Activate
  public void activate() {
    setPriority(15.9f);
    setDescription("custom Error Pages");
    setApplyOnModes("edit,preview,live");
    setApplyOnNodeTypes("jnt:customErrorPages");
  }

  @Override
  public String prepare(final RenderContext renderContext, final Resource resource, final RenderChain chain) {
    HttpServletRequest request = renderContext.getRequest();
    request.setAttribute(ERROR_PAGE_SETTINGS_PARAM, getErrorPageSettings(renderContext));
    return null;
  }

  public ErrorPageSettings getErrorPageSettings(final RenderContext renderContext) {
    final ErrorPageSettings errorSettings = new ErrorPageSettings();
    try {
      JCRSessionWrapper session = JCRSessionFactory.getInstance()
          .getCurrentUserSession(Constants.EDIT_WORKSPACE, renderContext.getMainResourceLocale());
      JCRNodeWrapper settingsNode = ErrorPageServiceImpl.getInstance().getSettingsNode(session, renderContext.getSite().getPath());
      if (settingsNode != null) {
        List<JCRNodeWrapper> nodes = JCRContentUtils.getChildrenOfType(settingsNode, ErrorPageService.ERROR_PAGE_CND_TYPE);
        JCRNodeWrapperImpl page = null;
        String title = null;
        for (JCRNodeWrapper node : nodes) {
          if (node.hasProperty(ErrorPageService.ERROR_PAGE_TARGET)) {
            page = (JCRNodeWrapperImpl) node.getProperty(ErrorPageService.ERROR_PAGE_TARGET).getNode();
            if (page.hasProperty(Constants.JCR_TITLE)) {
              title = page.getProperty(Constants.JCR_TITLE).getString();
            } else {
              title = page.getDisplayableName();
            }
            errorSettings.setTarget(node.getDisplayableName(), page.getIdentifier(), title);
          }
        }
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return errorSettings;
  }
}
