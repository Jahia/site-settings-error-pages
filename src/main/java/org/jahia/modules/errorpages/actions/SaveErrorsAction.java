package org.jahia.modules.errorpages.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.errorpages.service.ErrorPageService;
import org.jahia.modules.errorpages.service.ErrorPageServiceImpl;
import org.jahia.modules.errorpages.webflow.bean.ErrorPageSettings;
import org.jahia.modules.errorpages.webflow.bean.ErrorPageSettings.Target;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Action.class)
public class SaveErrorsAction extends Action {

  private final Logger LOGGER = LoggerFactory.getLogger(SaveErrorsAction.class);

  public SaveErrorsAction() {
    setName("saveErrorsAction");
    setRequiredMethods("GET,POST");
  }

  @Override
  public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, final Resource resource, JCRSessionWrapper session,
      final Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    String json = getParameter(parameters, "data");
    List<Map<String, Object>> list = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
    });

    ErrorPageSettings errorPageSettings = new ErrorPageSettings();
    for (Map<String, Object> item : list) {
      String status = (String) item.get("status");
      String identifier = (String) item.get("identifier");
      errorPageSettings.setTarget(status, identifier, "");
    }
    save(renderContext, errorPageSettings);

    return ActionResult.OK_JSON;
  }

  /**
   * Save Settings
   *
   * @param renderContext
   * @param settings
   */
  public void save(final RenderContext renderContext, final ErrorPageSettings settings) {
    String parentNodeId = null;
    try {
      parentNodeId = JCRTemplate.getInstance()
          .doExecuteWithSystemSessionAsUser(renderContext.getUser(), Constants.EDIT_WORKSPACE, renderContext.getMainResourceLocale(),
              new JCRCallback<String>() {
                public String doInJCR(JCRSessionWrapper session) throws RepositoryException {
                  JCRNodeWrapper settingsNode = ErrorPageServiceImpl.getInstance()
                      .getSettingsNode(session, renderContext.getSite().getPath());
                  if (settingsNode == null) {
                    settingsNode = renderContext.getSite()
                        .addNode(JCRContentUtils.findAvailableNodeName(renderContext.getSite(), ErrorPageService.SETTINGS_NODE_NAME),
                            ErrorPageService.SETTINGS_NODE_CND_TYPE);
                    renderContext.getSite().saveSession();
                  }

                  for (Target target : settings.getTargetList()) {
                    addSettings(session, settingsNode, target);
                  }

                  settingsNode.saveSession();
                  return settingsNode.getIdentifier();
                }
              });
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }

    // Publish
    if (org.apache.commons.lang.StringUtils.isNotBlank(parentNodeId)) {
      try {
        JCRPublicationService.getInstance().publishByMainId(parentNodeId);
      } catch (Exception e) {
        LOGGER.error("Cannot publish node : " + parentNodeId, e);
      }
    }
  }


  /**
   * Add settings for an error
   *
   * @param session
   * @param parentNode
   * @param config
   */
  private void addSettings(JCRSessionWrapper session, JCRNodeWrapper parentNode, Target config) {
    try {
      JCRNodeWrapper errorNode = ErrorPageServiceImpl.getInstance().getNode(session, parentNode.getPath() + "/" + config.getStatus());
      if (StringUtils.isBlank(config.getIdentifier())) {
        if (errorNode != null) {
          errorNode.remove();
        }
      } else {
        if (errorNode == null) {
          errorNode = parentNode.addNode(String.valueOf(config.getStatus()), ErrorPageService.ERROR_PAGE_CND_TYPE);
        }
        errorNode.setProperty(ErrorPageService.ERROR_PAGE_TARGET, new ValueImpl(config.getIdentifier(), PropertyType.WEAKREFERENCE, true));
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
