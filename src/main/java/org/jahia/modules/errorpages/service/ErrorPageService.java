package org.jahia.modules.errorpages.service;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.usermanager.JahiaUser;

/**
 * @author BEN AJIBA Youssef
 */
public interface ErrorPageService {
	String SETTINGS_NODE_NAME = "settings-error-pages";
	String SETTINGS_NODE_CND_TYPE = "jnt:settingsErrorPages";
	String ERROR_PAGE_CND_TYPE = "jnt:errorPage";
	String ERROR_PAGE_TARGET = "target";

	JCRNodeWrapper getSettingsNode(JCRSessionWrapper session, String sitePath) throws InvalidQueryException, RepositoryException;

	JCRNodeWrapper getNode(JCRSessionWrapper session, String path);

	RenderContext createRenderContext(HttpServletRequest req, HttpServletResponse resp, JahiaUser user);

	JCRUserNode lookupRootUser();
}
