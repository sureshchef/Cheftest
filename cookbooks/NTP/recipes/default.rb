#
# Cookbook Name:: NTP
# Recipe:: default
#
# Copyright 2013, YOUR_COMPANY_NAME
#
# All rights reserved - Do Not Redistribute
#
package 'ntp'

template '/etc/ntp.conf' do
   source 'ntp.conf.erb'
   notifies :restart , 'service[ntp]'
end

service 'ntp' do
  action [:enable, :start]
end

