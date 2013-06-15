#
# Cookbook Name:: fastflow
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#

include_recipe "apt"
include_recipe "mysql"
include_recipe "java"
include_recipe "build-essential"
include_recipe "openssl"
include_recipe "ark"

package "unzip"
package "libtool"
package "autoconf"
package "autogen" if platform_family?("debian")
package "gtar" if platform?("freebsd")

ark "fastflow" do
  version "2.0.0"
  url 'https://github.com/hrijulp/fastflow/blob/master/fastflow-2.0.0.tar.gz?raw=true'
  checksum '5996e676f17457c823d86f1605eaa44ca8a81e70d6a0e5f8e45b51e62e0c52e8'
  append_env_path true
  action :install
end

