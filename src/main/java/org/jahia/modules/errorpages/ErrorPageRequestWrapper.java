package org.jahia.modules.errorpages;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ErrorPageRequestWrapper extends HttpServletRequestWrapper {

    public ErrorPageRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public Map getParameterMap() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

}
