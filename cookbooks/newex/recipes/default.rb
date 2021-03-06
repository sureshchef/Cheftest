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
package "git"
package "ant"
package "gtar" if platform?("freebsd")

ark "play" do
 version "1.2.5"
 url 'http://downloads.typesafe.com/releases/play-1.2.5.zip?raw=true'
 action :install
end

#application "saymetrix" do
 # path "/home/chef/Downloads/saymetrix"
 # owner "root"
 # group "root"

  #repository "https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true"
 # revision "production"

  # Apply the rails LWRP from application_ruby
  #rails do
    # Rails-specific configuration. See the README in the
    # application_ruby cookbook for more information.
  #end

  # Apply the passenger_apache2 LWRP, also from application_ruby
 # passenger_apache2 do
    # Passenger-specific configuration.
 # end
#end

#ark "metrix" do
 # version "1.0.0"
 # url 'https://github.com/hrijulp/fastflow/blob/master/saymetrix.zip?raw=true'
 # action :install
#end

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

#execute "download" do

# if ::File.exist?"/saymetrixApp.zip"
 #  end

# command "wget https://www.dropbox.com/s/jfdwb4rfeklwuqv/saymetrixApp.zip"
#end




#execute "unzip" do

#if ::File.exist?"/saymetrix"
#end

 #command "unzip /saymetrixApp.zip"
#end
execute "clear" do
 cwd "/home/ubuntu"
 if ::File.exist?"/home/ubuntu/saymetrix"
  command "rm -r saymetrix"
 end
 
end

execute "Cloning" do
 cwd "/home/ubuntu"
 command "git clone https://github.com/sureshchef/saymetrix"
# cwd "/home/ubuntu/saymetrix"
# command "play deps"
# command "play test"
end

execute "Deps" do
  cwd "/home/ubuntu/saymetrix"
  command "clear"
  command "play deps"
 
end

execute "Test" do
  cwd "/home/ubuntu/saymetrix"
  command "nohup play test &"
end
