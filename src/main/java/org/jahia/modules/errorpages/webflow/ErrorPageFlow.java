package org.jahia.modules.errorpages.webflow;

import java.io.Serializable;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.errorpages.service.ErrorPageService;
import org.jahia.modules.errorpages.service.ErrorPageServiceImpl;
import org.jahia.modules.errorpages.webflow.bean.ErrorPageSettings;
import org.jahia.modules.errorpages.webflow.bean.ErrorPageSettings.Target;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRNodeWrapperImpl;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.jahia.services.render.RenderContext;
import org.slf4j.Logger;

/**
 * Webflow handler for the custom error pages
 *
 * @author BEN AJIBA Youssef
 */
public class ErrorPageFlow implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ErrorPageFlow.class);

    /**
     * Returns a new instance of model object to be used when displaying the form
     *
     * @param renderContext
     * @return ErrorPageSettings :
     */
    public ErrorPageSettings getErrorPageSettings(final RenderContext renderContext) {
        final ErrorPageSettings errorSettings = new ErrorPageSettings();
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, renderContext.getMainResourceLocale());
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

    /**
     * Save Settings
     *
     * @param renderContext
     * @param settings
     */
    public void save(final RenderContext renderContext, final ErrorPageSettings settings) {
        String parentNodeId = null;
        try {
            parentNodeId = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(renderContext.getUser(), Constants.EDIT_WORKSPACE, renderContext.getMainResourceLocale(),
                    new JCRCallback<String>() {
                public String doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper settingsNode = ErrorPageServiceImpl.getInstance().getSettingsNode(session, renderContext.getSite().getPath());
                    if (settingsNode == null) {
                        settingsNode = renderContext.getSite().addNode(JCRContentUtils.findAvailableNodeName(renderContext.getSite(), ErrorPageService.SETTINGS_NODE_NAME),
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
        if (StringUtils.isNotBlank(parentNodeId)) {
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
