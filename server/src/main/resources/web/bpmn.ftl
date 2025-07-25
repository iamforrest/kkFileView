<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <#include "*/commonHeader.ftl">
      <script src="js/base64.min.js" type="text/javascript"></script>
      	<#if currentUrl?contains("http://") || currentUrl?contains("https://") || currentUrl?contains("file://")>
    <#assign finalUrl="${currentUrl}">
  <#elseif currentUrl?contains("ftp://") >
   <#assign finalUrl="${currentUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${currentUrl}">
</#if>
    <style>
        html, body, #diagram {
            height: 100%
        }
    </style>
    <title>kkFileView Bpmn</title>
</head>
<body>

<h2>kkFileView Bpmn : ${fileName}</h2>

<div id="diagram"></div>

<!-- required modeler styles -->
<link rel="stylesheet" href="bpmn/diagram-js.css">
<link rel="stylesheet" href="bpmn/bpmn-js.css">
<link rel="stylesheet" href="bpmn/css/bpmn.css">

<!-- modeler distro -->
<script src="bpmn/bpmn-modeler.development.js"></script>
<script src="js/jquery-3.6.1.min.js"></script>

<!-- app -->
<script>

    const viewer = new BpmnJS({
        container: '#diagram'
    });
      var url = '${finalUrl}';
    var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
    if (!url.startsWith(baseUrl)) {
        url = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent('${WebUtils.encrypt(finalUrl)}');
    }
    async function showDiagram(diagramXML) {
        await viewer.importXML(diagramXML);
    }
    // load + show diagram
    $.get(url, showDiagram, 'text');
</script>
</body>
</html>
