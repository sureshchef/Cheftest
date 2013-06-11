#
# Cookbook Name:: saymatrix
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#

include_recipe "java::#{node['java']['install_flavor']}"

# Purge the deprecated Sun Java packages if remove_deprecated_packages is true
%w[sun-java6-jdk sun-java6-bin sun-java6-jre].each do |pkg|
  package pkg do
    action :purge
    only_if { node['java']['remove_deprecated_packages'] }
  end
end

