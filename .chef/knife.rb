log_level                :info
log_location             STDOUT
node_name                'ubuntu'
client_key               '/root/.chef/ubuntu.pem'
validation_client_name   'chef-validator'
validation_key           '/root/.chef/chef-validator.pem'
chef_server_url          'https://54.217.219.134'
syntax_check_cache_path  '/root/.chef/syntax_check_cache'
cookbook_path            '/home/ubuntu/chefproject/chef-repo/cookbooks/'
