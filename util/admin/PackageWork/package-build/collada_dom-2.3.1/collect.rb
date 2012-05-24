#! ruby -Ks

require 'yaml'
require 'FileUtils'
require 'pp'


CONFIG_YAML_FILE = 'config.yaml'
$config = YAML.load(open(CONFIG_YAML_FILE).read)

def collectFiles
  collectData = YAML.load(open($config['COLLECT_YAML_FILE']).read)
  for category in collectData
    oriPath =  category['path']
    name = category['name']
    category['files'].each_pair{|dir, files|
      for file in files

        ori = File.join(oriPath, file)
        dst_dir = File.join($config['HARVEST_PATH'], dir)
        
        unless FileTest.exist?(dst_dir)
          FileUtils.mkpath dst_dir
        end

        if FileTest::directory?(ori)
          FileUtils.cp_r(ori, dst_dir, {:verbose => true})
        else
          FileUtils.cp(ori, dst_dir, {:verbose => true})
        end
      end
    }
  end
end

collectFiles()

