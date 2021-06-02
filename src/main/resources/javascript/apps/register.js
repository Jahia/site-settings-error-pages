window.jahia.i18n.loadNamespaces('site-settings-error-pages');

window.jahia.uiExtender.registry.add('adminRoute', 'site-settings-error-pages', {
    targets: ['administration-sites:10'],
    isSelectable: true,
    icon: window.jahia.moonstone.toIconComponent('SiteWeb'),
    label: 'site-settings-error-pages:label.sitesettingserrorpages',
    requireModuleInstalledOnSite: 'site-settings-error-pages',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.manage-error-page-template.html'
});