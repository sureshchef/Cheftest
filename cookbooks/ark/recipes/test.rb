# require 'fileutils'

# remove file so we can test sending notification on its creation
if ::File.exist? "/tmp/foobarbaz/foo1.txt"
  FileUtils.rm_f "/tmp/foobarbaz/foo1.txt"
end

ruby_block "test_notification" do
  block do
    if ::File.exist? "/tmp/foobarbaz/foo1.txt"
      FileUtils.touch "/tmp/foobarbaz/notification_successful.txt"
    end
  end
  action :nothing
end

user 'foobarbaz'

directory "/opt/bin" do
  recursive true
end

ark "haproxy" do
  url  "http://haproxy.1wt.eu/download/1.5/src/snapshot/haproxy-ss-20120403.tar.gz"
  version "1.5"
  checksum 'ba0424bf7d23b3a607ee24bbb855bb0ea347d7ffde0bec0cb12a89623cbaf911'
  make_opts [ 'TARGET=linux26' ]
  action :install_with_make
end unless platform?("freebsd")

ark "test_autogen" do
  url 'https://github.com/zeromq/libzmq/tarball/master'
  extension "tar.gz"
  action :configure
  # autoconf in RHEL < 6 is too old
  not_if { platform_family?('rhel') && node['platform_version'].to_f < 6.0 }
end
