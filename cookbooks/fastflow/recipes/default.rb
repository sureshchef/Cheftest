#
# Cookbook Name:: fastflow
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

ZMQ_HOME = node['ZMQ']['ZMQ_HOME']
LD_LIBRARY_PATH = node['LD_LIBRARY']['LD_LIBRARY_PATH']


#do
#ENV['ZMQ_PATH='] = '/usr/local'
#ENV['LD_LIBRARY_PATH'] = '/usr/local/lib'
#ENV['LD_LIBRARY_PATH'] = '#{ENV['LD_LIBRARY_PATH']}:/usr/local/lib'
#end

ark "fastflow" do
    url 'https://github.com/hrijulp/fastflow/blob/master/fastflow-2.0.0.tar.gz?raw=true'
    checksum '89ba5fde0c596db388c3bbd265b63007a9cc3df3a8e6d79a46780c1a39408cb5'
    action :put
end

#ark "fastflow" do
 #version "2.0.0"
 # url 'https://github.com/hrijulp/fastflow/blob/master/fastflow-2.0.0.tar.gz?raw=true'
 # checksum '5996e676f17457c823d86f1605eaa44ca8a81e70d6a0e5f8e45b51e62e0c52e8'
 # action :install
#end

ark "haproxy" do
   url  "http://haproxy.1wt.eu/download/1.5/src/snapshot/haproxy-ss-20120403.tar.gz"
   version "1.5"
   checksum 'ba0424bf7d23b3a607ee24bbb855bb0ea347d7ffde0bec0cb12a89623cbaf911'
   make_opts [ 'TARGET=linux26' ]
   prefix_root '/opt'
   prefix_home '/opt'
   prefix_bin  '/opt/bin'
   action :install_with_make
end

ark "zeromq" do
   url 'https://github.com/zeromq/libzmq/tarball/master'
   extension "tar.gz"
   prefix_root '/usr/local'
   prefix_home '/usr/local/include'
   prefix_bin  '/usr/local/zeromq-1/autogen.sh'
   action :run
   action :configure
   action :install_with_make  
 
end


