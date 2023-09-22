package org.joget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;
import javax.servlet.http.HttpServletRequest;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListFilter;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class TreeMenu extends UserviewMenu {
    private final static String MESSAGE_PATH = "messages/TreeMenu";
    protected UserviewMenu innerMenu = null;
    protected DataListBinder binder = null;
    protected Map<String, Collection> data = null;

    @Override
    public String getCategory() {
        return "Marketplace";
    }

    @Override
    public String getIcon() {
        return "/plugin/org.joget.apps.userview.lib.HtmlPage/images/grid_icon.gif";
    }

    @Override
    public String getRenderPage() {
        String result = UserviewUtil.getUserviewMenuHtml(getInnerMenu());

        setProperty(UserviewMenu.ALERT_MESSAGE_PROPERTY,
                getInnerMenu().getPropertyString(UserviewMenu.ALERT_MESSAGE_PROPERTY));
        setProperty(UserviewMenu.REDIRECT_URL_PROPERTY,
                getInnerMenu().getPropertyString(UserviewMenu.REDIRECT_URL_PROPERTY));
        setProperty(UserviewMenu.REDIRECT_PARENT_PROPERTY,
                getInnerMenu().getPropertyString(UserviewMenu.REDIRECT_PARENT_PROPERTY));

        return result;
    }

    @Override
    public boolean isHomePageSupported() {
        return getInnerMenu().isHomePageSupported();
    }

    @Override
    public String getDecoratedMenu() {
        Map model = new HashMap();
        model.put("element", this);

        String label = getPropertyString("label");
        if (label != null) {
            label = StringUtil.stripHtmlRelaxed(label);
        }
        model.put("label", label);

        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            if (request != null) {
                request.setAttribute(getClassName(), true);
            }
            model.put("request", request);
        } catch (NoClassDefFoundError e) {
            // ignore if servlet request is not available
        }

        model.put("tree", getTree(""));

        // Retrieve coloring properties
        Object parentColor = getProperty("parentNodeTextColor");
        if (parentColor instanceof String)
            model.put("parentNodeColor", parentColor);

        Object childColor = getProperty("childNodeTextColor");
        if (childColor instanceof String)
            model.put("childNodeColor", childColor);

        Object parentIcon = getProperty("parentNodeIcon");
        if (parentIcon instanceof String) {
            parentIcon = retrieveClassName((String) parentIcon);
            model.put("parentNodeIcon", parentIcon);
        }

        Object childIcon = getProperty("childNodeIcon");
        if (childIcon instanceof String) {
            childIcon = retrieveClassName((String) childIcon);
            model.put("childNodeIcon", childIcon);
        }

        String content = pluginManager.getPluginFreeMarkerTemplate(model, getClass().getName(),
                "/templates/treeMenu.ftl", null);
        return content;
    }

    private String retrieveClassName(String html) {
        Pattern classPattern = Pattern.compile("class=\"(.*?)\"");

        Matcher matcher = classPattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        } else 
            return "";
    }

    public String getName() {
        return "Tree Menu";
    }

    public String getVersion() {
        return "8.0.0";
    }

    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.TreeMenu.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.TreeMenu.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/treeMenu.json", null, true, MESSAGE_PATH);
    }

    protected UserviewMenu getInnerMenu() {
        if (innerMenu == null) {
            Object menuData = getProperty("innerMenu");
            if (menuData != null && menuData instanceof Map) {
                Map menuMap = (Map) menuData;
                if (menuMap.containsKey("className") && !menuMap.get("className").toString().isEmpty()) {
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext()
                            .getBean("pluginManager");
                    innerMenu = (UserviewMenu) pluginManager.getPlugin(menuMap.get("className").toString());

                    if (innerMenu != null) {
                        Map menuProps = (Map) menuMap.get("properties");
                        innerMenu.setProperties(menuProps);

                        innerMenu.setRequestParameters(getRequestParameters());
                        innerMenu.setUserview(getUserview());
                        innerMenu.setProperty("menuId", getPropertyString("menuId"));

                        String url = getUrl();

                        Object[] treeNodeParams = null;
                        if (getProperty("treeNodeParams") instanceof Object[]) {
                            treeNodeParams = (Object[]) getProperty("treeNodeParams");

                            for (Object o : treeNodeParams) {
                                Map mapping = (HashMap) o;
                                String param = mapping.get("paramName").toString();
                                url = StringUtil.addParamsToUrl(url, param, getRequestParameterString(param));
                            }
                        }

                        innerMenu.setUrl(url);
                    }
                }
            }
        }

        return innerMenu;
    }

    protected DataListBinder getBinder() {
        if (binder == null) {
            Object binderData = getProperty("binder");
            if (binderData != null && binderData instanceof Map) {
                Map bdMap = (Map) binderData;
                if (bdMap != null && bdMap.containsKey("className") && !bdMap.get("className").toString().isEmpty()) {
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext()
                            .getBean("pluginManager");
                    binder = (DataListBinder) pluginManager.getPlugin(bdMap.get("className").toString());

                    if (binder != null) {
                        Map bdProps = (Map) bdMap.get("properties");
                        binder.setProperties(bdProps);
                    }
                }
            }
        }

        return binder;
    }

    protected String getTree(String parentId) {
        Collection<Object> nodes = getData().get(parentId);
        Boolean isParent = false;
        if (parentId.equals(""))
            isParent = true;
        if (nodes != null && !nodes.isEmpty()) {
            String html = "<ul>";

            for (Object n : nodes) {
                String id = (String) DataListService.evaluateColumnValueFromRow(n,
                        getBinder().getPrimaryKeyColumnName());
                String label = (String) DataListService.evaluateColumnValueFromRow(n,
                        getPropertyString("treeNodeLabel"));
                String url = getUrl();

                Object[] treeNodeParams = null;
                if (getProperty("treeNodeParams") instanceof Object[]) {
                    treeNodeParams = (Object[]) getProperty("treeNodeParams");

                    for (Object o : treeNodeParams) {
                        Map mapping = (HashMap) o;
                        String param = mapping.get("paramName").toString();
                        String colId = mapping.get("column").toString();
                        String value = mapping.get("defaultValue").toString();
                        try {
                            String nvalue = (String) DataListService.evaluateColumnValueFromRow(n, colId);
                            if (nvalue != null) {
                                value = nvalue;
                            }
                        } catch (Exception ex) {
                        }
                        url = StringUtil.addParamsToUrl(url, param, value);
                    }
                }

                html += "<li id=\"" + StringUtil.stripAllHtmlTag(id) + "\" data-jstree=\'{\"type\":\""
                        + (isParent ? "parent" : "child") + "\"}\'>"
                        + "<a href=\"" + url + "\">" + StringUtil.stripHtmlRelaxed(label)
                        + "</a>" + getTree(id) + "</li>";
            }

            html += "</ul>";
            return html;
        }
        return "";
    }

    protected Map<String, Collection> getData() {
        if (data == null) {
            data = new HashMap<String, Collection>();

            try {
                int depth = 2;
                try {
                    depth = Integer.parseInt(getPropertyString("depth"));
                } catch (Exception e) {
                }

                String orderBy = null;
                Boolean order = null;

                String orderByColumnId = getPropertyString("orderBy");
                if (!orderByColumnId.isEmpty()) {
                    orderBy = getBinder().getColumnName(orderByColumnId);
                    order = getPropertyString("order").equals("true");
                }

                Collection<String> currentLevelParentIds = null;

                for (int level = 0; level < depth; level++) {
                    if (currentLevelParentIds == null) {
                        String rootId = getPropertyString("rootParentId");
                        if (!rootId.isEmpty()) {
                            currentLevelParentIds = new ArrayList<String>();
                            currentLevelParentIds.add(rootId);
                        }
                    }

                    DataListCollection nodes = getBinder().getData(null, getBinder().getProperties(),
                            new DataListFilterQueryObject[] { getFilter(currentLevelParentIds) }, orderBy, order, null,
                            null);

                    if (nodes == null || nodes.isEmpty()) {
                        break;
                    } else {
                        currentLevelParentIds = new ArrayList<String>();
                        for (Object r : nodes) {
                            String parentId = "";
                            if (level != 0) {
                                parentId = (String) DataListService.evaluateColumnValueFromRow(r,
                                        getPropertyString("treeNodeParentId"));
                            }

                            Collection<Object> childs = data.get(parentId);
                            if (childs == null) {
                                childs = new ArrayList<Object>();
                                data.put(parentId, childs);
                            }
                            childs.add(r);

                            String id = (String) DataListService.evaluateColumnValueFromRow(r,
                                    getBinder().getPrimaryKeyColumnName());
                            currentLevelParentIds.add(id);
                        }
                    }
                }
            } catch (Exception ex) {
                LogUtil.error(getClassName(), ex, "Not able to get data for tree rendering.");
            }
        }
        return data;
    }

    protected DataListFilterQueryObject getFilter(Collection<String> ids) {
        DataListFilterQueryObject filterQueryObject = new DataListFilterQueryObject();
        filterQueryObject.setOperator(DataListFilter.OPERATOR_AND);
        String sql = "";
        Collection<String> values = new ArrayList<String>();

        if (ids != null && ids.size() > 0) {
            sql += "(";
            int i = 0;
            for (String id : ids) {
                if (i % 1000 == 0) {
                    if (i > 0) {
                        sql += " OR ";
                    }
                    sql += getBinder().getColumnName(getPropertyString("treeNodeParentId")) + " in (";
                }

                sql += "?,";
                values.add(id);

                if (i % 1000 == 999 || i == ids.size() - 1) {
                    sql = sql.substring(0, sql.length() - 1) + ")";
                }
                i++;
            }
            sql += ")";
        } else {
            sql += "(" + getBinder().getColumnName(getPropertyString("treeNodeParentId")) + " is null or "
                    + getBinder().getColumnName(getPropertyString("treeNodeParentId")) + " = '')";
        }
        filterQueryObject.setQuery(sql);
        filterQueryObject.setValues(values.toArray(new String[0]));

        return filterQueryObject;
    }
}
