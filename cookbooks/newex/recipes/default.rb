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
  command "cp -r /usr/local/play-1.2.5 /home/ubuntu/"
end

execute "chmod" do
  command "chmod a+x /home/ubuntu/play-1.2.5/play"
end

execute "export" do
  command "export PATH=$PATH:/home/ubuntu/play-1.2.5/play"
end

execute "remove" do
  command "rm /usr/bin/play"
end

execute "link" do
  command "ln -s /home/ubuntu/play-1.2.5/play /usr/bin/play"
end

ark "saymetrix" do
  url 'https://www.dropbox.com/s/209i3l3bcwsf6xy/saymetrix.zip?raw=true'
  action :install
end

execute "deps" do 
  command "/usr/local/saymetrix/play deps"
end

execute "test" do
  command "/usr/local/saymetrix/play test"
end
