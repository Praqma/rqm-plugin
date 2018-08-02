---
maintainer: MadsNielsen
---

rqm-plugin
=================

This plugin will expose any attribute added to an automatic test script from RQM as  enviroment variable.

The plugin then allows you to add a build step for each of these scripts, wherein this metadata is available for use. For example if your test script has a custom attribute called staging which can be `true` or `false`. You'll be able to reference this in the build step as an enviroment variable using `%STAGING%` when used with `Execute Windows Batch Script` build step. 

The plugin summarize the results as a whole so if one of the embedded builds step fails, it will continue with the next embedded build step, but once all steps are executed it will mark the overall results as the worst result from any of the iterations. 
