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
include_recipe "ark"
include_recipe "application"
include_recipe "play"

#execute "update package index" do
 # command "gem install passenger"
 # ignore_failure true
 # action :nothing
 # end.run_action(:run)

application "saymetrix" do
  path "/home/chef/Downloads/saymetrix"
  owner "ubuntu"
  group "app-group"

  repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
  revision "production"

  # Apply the rails LWRP from application_ruby
  #rails do
    # Rails-specific configuration. See the README in the
    # application_ruby cookbook for more information.
  #end

  # Apply the passenger_apache2 LWRP, also from application_ruby
 # passenger_apache2 do
    # Passenger-specific configuration.
 # end
end


application "saymetrix" do
  repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
  revision node.chef_environment == "production" ? "production" : "develop"
end

application "my_app" do
  restart_command "kill -1 `cat /var/run/one.pid`"
  environment "LC_ALL" => "en", "FOO" => "bar"
end 

 # rails do
  #  restart_command "touch /tmp/something"
  #  environment "LC_ALL" => "en_US"
  #end

  #passenger_apache2 do
  #  environment "deault" => "chef"
  #end
#end


