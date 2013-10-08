
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

execute "fastflow" do
    cwd node['FastFlow']['node']
     command 'wget https://www.dropbox.com/s/p8lnndsek2lbcmh/FastFlow-VIP.zip'
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

execute "update" do
command "sudo apt-get update"
end

execute "cmake" do
command "sudo apt-get -y install cmake libblkid-dev e2fslibs-dev libboost-all-dev libaudit-dev g++ libtool autoconf automake uuid-dev unzip  libjson-spirit-dev"
end

execute "unzip" do
cwd node['FastFlow']['node']
command "sudo unzip FastFlow-VIP.zip"
end

execute "json" do
cwd node['json']['node'] 
command "sudo mkdir build"
end

execute "json" do
cwd node['json']['build']
command "sudo cmake .."
end

execute "intel" do
cwd node['ocl']['intel']
command "wget https://www.dropbox.com/s/a0o2xdw2e9atlld/intel_ocl_sdk_2012_x64.rpm"
end

execute "update" do
command "sudo apt-get update" 
end

execute "libnuma" do
command "sudo apt-get -y  install rpm alien libnuma1 fakeroot mesa-common-dev"
end

execute "fakeroot" do
cwd node['ocl']['intel']
command "sudo fakeroot alien --to-deb intel_ocl_sdk_2012_x64.rpm"
end

execute "" do
cwd node['ocl']['intel']
command "sudo dpkg -i intel-ocl-sdk_2.0-31361_amd64.deb"
end

execute "link" do
command "sudo ln -s /usr/lib64/libOpenCL.so /usr/lib/libOpenCL.so"
end


execute "copy" do
  command "sudo cp /usr/local/zeromq-1/include/* /usr/local/include/"
end


execute "configure" do
  cwd node['ZMQ']['path']
  command "sudo ./configure"
end

execute "make" do
  command "sudo make"
end

execute "make install" do
  command "sudo make install"
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

execute "app1" do
  cwd node['VIP']['test']
  command "sudo make"
end

