#
# Cookbook Name:: newex
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#


include_recipe "apt"
include_recipe "java"
include_recipe "openssl"
include_recipe "ark"
include_recipe "apache2"


package "unzip"
package "sox"
package "ant"
package "gtar" if platform?("freebsd")

ark "play" do
 version "1.2.5"
 url 'http://downloads.typesafe.com/releases/play-1.2.5.zip?raw=true'
 action :install
end

execute "copy" do
  command "cp /usr/local/play-1.2.5 /home/ubuntu/"
end

execute "chmod" do
  command "chmod a+x /home/ubuntu/play-1.2.5/play"
end

execute "export" do
  command "export PATH=$PATH:/home/ubuntu/play-1.2.5/play"
end

