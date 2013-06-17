
ruby_block  "set-env-ZMQ_HOME" do
  block do
    ENV["ZMQ_HOME"] = node['ZMQ']['ZMQ_HOME']
  end
  not_if { ENV["ZMQ_HOME"] == node['ZMQ']['ZMQ_HOME'] }
end

ruby_block  "set-env-LD_LIBRARY_PATH" do
  block do
    ENV["LD_LIBRARY_PATH"] = node['LD_LIBRARY']['LD_LIBRARY_PATH']
  end
  not_if { ENV["LD_LIBRARY_PATH"] == node['LD_LIBRARY']['LD_LIBRARY_PATH'] }
end
