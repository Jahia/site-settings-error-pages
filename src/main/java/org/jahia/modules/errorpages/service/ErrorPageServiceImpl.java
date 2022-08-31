package org.jahia.modules.errorpages.service;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jahia.exceptions.JahiaBadRequestException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;

/**
 * This Service is used to manage error pages.
 *
 * @author BEN AJIBA Youssef
 */
public class ErrorPageServiceImpl implements ErrorPageService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ErrorPageServiceImpl.class);
    private JahiaUserManagerService jahiaUserManagerService;

    private static class Holder {

        static final ErrorPageServiceImpl INSTANCE = new ErrorPageServiceImpl();
    }

    public static ErrorPageServiceImpl getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * get parent node used to save error pages settings
     *
     * @return JCRNodeWrapper : the parent node.
     */
    @Override
    public JCRNodeWrapper getSettingsNode(JCRSessionWrapper session, String sitePath) throws RepositoryException {
        StringBuilder sQuery = new StringBuilder();
        sQuery.append("SELECT * FROM [" + SETTINGS_NODE_CND_TYPE + "] WHERE");
        sQuery.append(" ISDESCENDANTNODE('" + sitePath + "')");
        Query query = session.getWorkspace().getQueryManager().createQuery(sQuery.toString(), Query.JCR_SQL2);
        query.setLimit(1);
        NodeIterator nodeIterator = query.execute().getNodes();
        if (nodeIterator.hasNext()) {
            return (JCRNodeWrapper) nodeIterator.next();
        }
        return null;
    }

    @Override
    public JCRNodeWrapper getNode(JCRSessionWrapper session, String path) {
        JCRNodeWrapper node = null;
        try {
            node = session.getNode(path);
        } catch (RepositoryException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return node;
    }

    @Override
    public RenderContext createRenderContext(HttpServletRequest req, HttpServletResponse resp, JahiaUser user) {
        RenderContext context = new RenderContext(req, resp, user);
        int index = req.getPathInfo().indexOf("/", 1);
        if (index == -1 || index == req.getPathInfo().length() - 1) {
            throw new JahiaBadRequestException("Invalid path");
        }
        context.setServletPath(req.getServletPath() + req.getPathInfo().substring(0, index));
        return context;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    /**
     * Returns the system root user (not cached).
     *
     * @return the system root user (not cached)
     */
    @Override
    public JCRUserNode lookupRootUser() {
        return jahiaUserManagerService.lookupRootUser();
    }
}
