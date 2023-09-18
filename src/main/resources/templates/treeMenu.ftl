<#if !(request.getAttribute("org.joget.TreeMenu")??)>
    <link rel="stylesheet" href="${request.contextPath}/js/jstree/themes/default/style.min.css"/>
    <script type="text/javascript" src="${request.contextPath}/js/jstree/jstree.min.js"></script>
    <style>
        #category-container div.tree_menu ul {
            display: block;
            position: relative !important;
            width: 100% !important;
        }
        #category-container div.tree_menu ul li[data-jstree='{"type":"parent"}'] a {
            padding: 0 4px 0 1px !important;
            display: inline-block;
            color: ${parentNodeColor!};
        }
        #category-container div.tree_menu ul li[data-jstree='{"type":"child"}'] a {
            padding: 0 4px 0 1px !important;
            display: inline-block;
            color: ${childNodeColor!};
        }
    </style>
</#if>
<a class='menu-link default'>
    <span>${label!}</span>
</a>
<div id="treemenu_${element.properties.id!}" class="tree_menu sidemenu_inner">
    ${tree!}
</div>
<script>
    $(document).ready(function () {
        $('#treemenu_${element.properties.id!}').jstree({
            // Add the code to be able to manipulate the tree menu using themes uploaded.
            "plugins": ["state", "types", "themes"],
            "state": {
                "key": "treemenu_${element.properties.id!}"
            },
            "types":{
                "parent":{
                    "icon":"${parentNodeIcon!}"
                },
                "child":{
                    "icon":"${childNodeIcon!}"
                }
            }
        }).on('loaded.jstree', function () {
            $('#treemenu_${element.properties.id!}').jstree('open_all');
        });
        if (window.location.href.indexOf('${element.url}') === -1) {
            setTimeout(function () {
                $('#treemenu_${element.properties.id!}').jstree("deselect_all");
            }, 100);
        }
        $('#treemenu_${element.properties.id!}').on('click', 'a.jstree-anchor', function () {
            window.location = $(this).attr('href');
        });
    });
</script>
