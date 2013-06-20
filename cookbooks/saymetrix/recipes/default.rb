#
# Cookbook Name:: saymetrix
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#

include_recipe "apt"
include_recipe "java"
include_recipe "build-essential"
include_recipe "openssl"
include_recipe "ark"
include_recipe "subversion"
include_recipe "application"
include_recipe "play"
include_recipe "maven"



#deploy "/usr/local/saymetrix" do
#  repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
# provider Chef::Provider::Deploy::Revision
#  action [:deploy]       
#  retries 0              
#  retry_delay 2
#  deploy_to "/opt/my_deploy" 
#end


application "saymetrix_app" do
 name "saymetrix"
 path "/usr/local"
 owner "chef"
 group "chef"
 repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
 revision "production"
end



#application "saymetrix" do
#  repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
#  revision node.chef_environment == "production" ? "production" : "develop"
# end
