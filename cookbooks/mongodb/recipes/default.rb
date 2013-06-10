#
# Cookbook Name:: mongodb
# Recipe:: default
#
# Copyright 2011, edelight GmbH
# Authors:
#       Markus Korn <markus.korn@edelight.de>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

package "mongodb" do
  action :install
end

## Firewall configuration ##
#
# in order to find all member nodes of a mongodb cluster you have to run queries
# like:
#    source_nodes = []
#
#    node['mongodb']['client_roles'].each do |client_role|
#      source_nodes += search(:node, "role:#{client_role} AND chef_environment:#{node.chef_environment}")
#    end
#
#    if !node['mongodb']['cluster_name'].nil?
#      source_nodes += search(
#        :node,
#        "mongodb_cluster_name:#{node['mongodb']['cluster_name']} AND \
#         (NOT ipaddress:#{node['ipaddress']}) AND \
#         chef_environment:#{node.chef_environment}"
#      )
#    end
##
