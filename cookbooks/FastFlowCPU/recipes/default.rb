
# Cookbook Name:: fastflow
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#

include_recipe "apt"
include_recipe "build-essential"
include_recipe "openssl"
include_recipe "ark"
#include_recipe "subversion"

ZMQ_HOME = node['ZMQ']['ZMQ_HOME']
LD_LIBRARY_PATH = node['LD_LIBRARY']['LD_LIBRARY_PATH']


#do
#ENV['ZMQ_PATH='] = '/usr/local'
#ENV['LD_LIBRARY_PATH'] = '/usr/local/lib'
#ENV['LD_LIBRARY_PATH'] = '#{ENV['LD_LIBRARY_PATH']}:/usr/local/lib'
#end

ark "fastflow" do
     url 'https://github.com/sureshchef/Cheftest/blob/master/fastflow-2.0.0.tar.gz?raw=true'
    action :put
end

#ark "fastflow" do
 #version "2.0.0"
 # url 'https://github.com/sureshchef/Cheftest/blob/master/fastflow-2.0.0.tar.gz?raw=true'
  # action :install_with_make
#end


#ark "haproxy" do
 #  url  "http://haproxy.1wt.eu/download/1.5/src/snapshot/haproxy-ss-20120403.tar.gz"
 #  version "1.5"
 #  checksum 'ba0424bf7d23b3a607ee24bbb855bb0ea347d7ffde0bec0cb12a89623cbaf911'
 #  make_opts [ 'TARGET=linux26' ]
 #  prefix_root '/opt'
 #  prefix_home '/opt'
 #  prefix_bin  '/opt/bin'
 #  action :install_with_make
#end

ark "zeromq" do
  url 'http://download.zeromq.org/zeromq-2.2.0.tar.gz'
  extension "tar.gz"
  prefix_root '/usr/local'
  prefix_home '/usr/local/include'
  prefix_bin  '/usr/local/include'
  action :install_with_make
end

#ark "zeromq" do
 # url 'http://download.zeromq.org/zeromq-2.2.0.tar.gz'
 # prefix_root '/usr/local'
 # prefix_home '/usr/local/include'
 # prefix_bin  '/usr/local/include'
 # action :install_with_make
#end


require_recipe 'build-essential'

package "wget" do
  action :install
end

execute "copy" do
  command "cp /usr/local/zeromq-1/include/* /usr/local/include/"
end


execute "configure" do
  cwd ['ZMQ']['path']
  command "./configure"
end

execute "make" do
  command "make"
end

execute "make install" do
  command "make install"
end

file "/etc/ld.so.conf.d/libc.conf" do
  owner "root"
  group "root"
  mode 0644
  content "/usr/local/lib"
end

execute "ldconfig" do
  command "/sbin/ldconfig"
end

execute "compile" do
  command "g++ -I. -I/usr/local/fastflow -I/usr/local -DNO_CMAKE_CONFIG -Wall -g -o /usr/local/fastflow/tests/d/pipe_farm2 /usr/local/fastflow/tests/d/pipe_farm2.cpp -L /usr/local/lib -lzmq -lpthread"
end

