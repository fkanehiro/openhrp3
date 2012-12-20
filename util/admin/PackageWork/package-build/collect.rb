#! ruby -Ks

require 'yaml'
require 'FileUtils'
require 'pp'


#CONFIG_YAML_FILE = 'config.yaml'
CONFIG_YAML_FILE = ARGV[0]
$config = YAML.load(open(CONFIG_YAML_FILE).read)


def copyFiles()
  FileUtils.rm_rf($config['HARVEST_PATH'], {:verbose => true})
  FileUtils.cp_r($config['SOURCE_PATH'], $config['HARVEST_PATH'], {:verbose => true})
end

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


def margeFiles
  margeData = YAML.load(open($config['MARGE_YAML_FILE']).read)
  for marge in margeData
    unless FileUtils.cmp(marge['origin'], marge['comp'])
      raise "変更元ファイルのバージョンが変わっております。確認してください。#{marge['origin']}"
    end

    dst_dir = File.join($config['HARVEST_PATH'], marge['dest'])
    unless FileTest.exist?(dst_dir)
      FileUtils.mkpath dst_dir
    end
    FileUtils.cp(marge['src'], dst_dir, {:verbose => true})
  end
end

  
copyFiles()
collectFiles()
if ARGV[1] == "marge" then
	margeFiles()
end
