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

