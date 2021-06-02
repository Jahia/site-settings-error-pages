<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib uri="http://www.jahia.org/tags/templateLib" prefix="template"%>
<%@ taglib uri="http://www.jahia.org/tags/uiComponentsLib" prefix="ui"%>
<%@ taglib uri="http://www.jahia.org/tags/utilityLib" prefix="utility"%>
<%@ taglib uri="http://www.jahia.org/tags/queryLib" prefix="query"%>
<%@ taglib uri="http://www.jahia.org/tags/functions" prefix="functions"%>
<%@ taglib uri="http://www.jahia.org/tags/jcr" prefix="jcr"%>

<template:addResources>
	<script type="text/javascript">
		function clearValues(element) {
			if (element) {
				var tr = $(element).closest("tr");
				tr.find("input[type=hidden]").first().val("");
				tr.find("span").first().text("");
			}
		}
	</script>
</template:addResources>

<fmt:message key="siteSettings.customErrorPages.select" var="selectLabel" />
<fmt:message key="siteSettings.customErrorPages.modify" var="modifyLabel" />

<h2>
	<fmt:message key="siteSettings.customErrorPages" />
</h2>
<br />
<form name="jahiaAdmin" action='${flowExecutionUrl}' method="post">
	<div class="box-1" style="width: 50%;">
		<table class="table table-bordered table-striped table-hover">
			<thead>
				<tr>
					<th width="10%">
						<fmt:message key="siteSettings.customErrorPages.error" />
					</th>
					<th width="70%">
						<fmt:message key="siteSettings.customErrorPages.pagename" />
					</th>
					<th width="20%" colspan="3">
						<fmt:message key="siteSettings.customErrorPages.action" />
					</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${errorPageSettings.targetList}" var="field" varStatus="loop">
					<tr>
						<td>${field.status}</td>
						<td>
							<input type="hidden" name="targetList[${loop.index}].identifier" id="input_${field.status}" value="${field.identifier}" />
							<span id="label_${field.status}">${field.title}</span>
						</td>
						<td style="text-align: center;">
							<ui:pageSelector fieldId="input_${field.status}" label="${empty field.identifier ? selectLabel : modifyLabel}" displayFieldId="label_${field.status}" includeChildren="false"
								displayIncludeChildren="false" valueType="identifier" />
						</td>
						<td>
							<a class="btn btn-danger btn-small" title="Supprimer" onclick="clearValues(this);">
								<i class="icon-remove icon-white"></i>
							</a>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</div>

	<button class="btn btn-primary" type="submit" name="_eventId_submit">
		<i class="icon-ok icon-white"></i>
		&nbsp;
		<fmt:message key="label.save" />
	</button>
</form>