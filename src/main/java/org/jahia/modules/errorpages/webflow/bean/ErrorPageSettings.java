package org.jahia.modules.errorpages.webflow.bean;

import java.io.Serializable;

import org.apache.commons.httpclient.HttpStatus;
import org.codehaus.plexus.util.StringUtils;

/**
 * Model object for setting up error pages
 * 
 * @author BEN AJIBA Youssef
 *
 */
public class ErrorPageSettings implements Serializable {
	/**
	 * Serial ID
	 */
	private static final long serialVersionUID = 1L;

	private final Target[] targetList = new Target[] { new Target(HttpStatus.SC_BAD_REQUEST), new Target(HttpStatus.SC_UNAUTHORIZED), new Target(HttpStatus.SC_FORBIDDEN),
			new Target(HttpStatus.SC_NOT_FOUND), new Target(HttpStatus.SC_INTERNAL_SERVER_ERROR) };

	public Target[] getTargetList() {
		return targetList;
	}

	public static class Target implements Serializable {
		/**
		 * Serial ID
		 */
		private static final long serialVersionUID = 1L;
		private int status;
		private String identifier;
		private String title;

		public Target(int status) {
			this.status = status;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	public void setTarget(String errorStatus, String identifier, String title) {
		if (StringUtils.isNotBlank(errorStatus)) {
			for (Target target : targetList) {
				if (errorStatus.equals(String.valueOf(target.getStatus()))) {
					target.setIdentifier(identifier);
					target.setTitle(title);
				}
			}
		}
	}
}