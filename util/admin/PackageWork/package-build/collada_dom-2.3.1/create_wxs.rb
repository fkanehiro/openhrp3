#! ruby -Ks
# -*- coding: utf-8 -*-

require 'yaml'
require 'Win32API'
require 'Singleton'

Encoding.default_external = 'UTF-8'
INDENT_STR = "  "

class RootDir
  include Singleton
  
  attr_reader :dirId, :rootName, :allDirs, :allFiles, :rootPath, :children, :files, :comps, :features, :defFeatComps

  def initialize
    @indentCount = 2
    @dirId = nil
    @rootPath = nil
    @features = []
    @allDirs = []
    @allFiles = []
    @children = []
    @files = []
    @comps = []
    @defFeatComps = []
  end

  def init(rootId, rootName, rootPath)
    raise if @dirId || @rootPath
    
    @dirId = rootId
    @rootName = rootName
    @rootPath = rootPath
    @defaultFeatureId = RootDir.getFeatureId

    ds = Dir.glob(rootPath + "/**/*").select{|p| FileTest::directory?(p)}
    ds.collect!{|p| p.sub(/^#{rootPath}\//, "")}

    ds.each{|tgt|
      if @allDirs.find{|sd| sd.path == tgt}
        next
      end
      
      sd = SourceDir.new(tgt)
      if File.dirname(tgt) == "." then
        children.push(sd)
      else
        parent = @allDirs.find{|sd| sd.path == File.dirname(tgt)}
        parent.pushdir(sd)
      end
      @allDirs.push(sd)
    }
    
    fs = Dir.glob(rootPath + "/**/*").select{|p| FileTest::file?(p)}
    fs.collect!{|p| p.sub(/^#{rootPath}\//, "")}
    fs.each{|tgt|
      if File.dirname(tgt) == "." then
        sf = SourceFile.new(tgt, self)
        addComp(sf.comp)
        @allFiles.push(sf)
      else
        parent = @allDirs.find{|sd| sd.path == File.dirname(tgt)}
        sf = SourceFile.new(tgt, parent)
        addComp(sf.comp)
        parent.pushfile(sf)
        @allFiles.push(sf)
      end
    }
  end
  
  public
  def addFeature(title, description, paths)
    feature = Feature.new(title, description, paths)
    @features.push(feature)
  end

  def addEnvironment(name, value, action, part, system)
    env = Environment.new(name, value, action, part, system)
    @comps.push(env.comp)
    @defFeatComps.push(env.comp)
  end
  def wxs_dir_str
    str = ""
    str += INDENT_STR * @indentCount + %Q!<Directory Id='TARGETDIR' Name='SourceDir'>\n!
    str += INDENT_STR * (@indentCount + 1) + %Q!<Directory Id='ProgramFilesFolder' Name='PFiles'>\n!
    str += INDENT_STR * (@indentCount + 2) + %Q!<Directory Id='#{@dirId}' Name='#{@rootName}'>\n!
    for child in @children
      str += child.wxs_dir_str(@indentCount + 3)
    end
    str += INDENT_STR * (@indentCount + 2) + "</Directory>\n"
    str += INDENT_STR * (@indentCount + 1) + "</Directory>\n"
    str += INDENT_STR * @indentCount + "</Directory>\n"
    return str
  end

  def wxs_component_str
    str = "" 
    for comp in @comps
      str += comp.wxs_str(@indentCount)
    end
    return str
  end
  def wxs_feature_str
    str = ""
    str += INDENT_STR * @indentCount + %Q!<Feature Id='#{@defaultFeatureId}' Title='OpenHRP-SDK' Description='OpenHRP-SDK complete package' TypicalDefault='install' Display='expand' AllowAdvertise='no' ConfigurableDirectory= 'INSTALLLOCATION' Absent='disallow' Level='1'>\n!
    for comp in @defFeatComps
      str += INDENT_STR * (@indentCount + 1) + %Q!<ComponentRef Id='#{comp.compId}' />\n!
    end
    for feature in @features
      str += feature.wxs_str(@indentCount + 1)
    end
    
    str += INDENT_STR * @indentCount + "</Feature>"
  end
  
  def addComp(comp)
    is_add = false
    for feature in @features
      if feature.addComp(comp)
        is_add = true
      end
    end

    unless is_add
      @defFeatComps.push(comp)
    end
    @comps.push(comp)
  end
  
  private
  @@fid = 1
  @@did = 1
  @@cid = 1
  @@eid = 1
  @@feid = 1
  @@uuidCreate = Win32API.new('rpcrt4', 'UuidCreate', 'P', 'L')
  
  public
  def RootDir.getDirID
    id = "Dir#{sprintf("%05d", @@did)}"
    @@did += 1
    return id
  end
  def RootDir.getFileID
    id = "File#{sprintf("%05d", @@fid)}"
    @@fid += 1
    return id
  end
  def RootDir.getComponentID
    id = "Comp#{sprintf("%05d", @@cid)}"
    @@cid += 1
    return id
  end
  def RootDir.getEnvironmentID
    id = "Env#{sprintf("%05d", @@eid)}"
    @@eid += 1
    return id
  end
  def RootDir.getFeatureId
    id = "Feat#{sprintf("%05d", @@feid)}"
    @@feid += 1
    return id
  end
  def RootDir.newGuid
    result = ' ' * 16
    @@uuidCreate.call(result)
    a, b, c, d, e, f, g, h = result.unpack('SSSSSSSS')
    sprintf('{%04X%04X-%04X-%04X-%04X-%04X%04X%04X}', a, b, c, d, e, f, g, h)
  end
end

class SourceDir
  attr_reader :path, :children, :dirId, :dirName, :files
  def initialize(path)
    @children = []
    @files = []
    @path = path
    @dirId = RootDir.getDirID
    @dirName = File.basename(@path)
  end
  def pushdir(child)
    @children.push(child)
  end
  def pushfile(file)
    @files.push(file)
  end

  def wxs_dir_str(indentCount)
    str = INDENT_STR * indentCount + %Q!<Directory Id='#{@dirId}' Name='#{@dirName}'>! + "\n"
    for child in children
      str += child.wxs_dir_str(indentCount + 1)
    end
    str += INDENT_STR * indentCount + "</Directory>\n"
    return str
  end
end

class Environment
  attr_reader :id, :name, :value, :action, :part, :system, :comp
  def initialize(name, value, action, part, system)
    @id = RootDir.getEnvironmentID
    @name = name
    @value = value
    @action = action
    @part = part
    @system = system
    @comp = Component.new(RootDir.instance, self)
    @comp.withKeyPath = true
  end
  
  def wxs_str(indentCount)
    str = INDENT_STR * indentCount + %Q!<Environment Id='#{@id}' Action='#{@action}' Name='#{@name}' Part='#{@part}' System='#{system}' Permanent='no' Value='#{@value}' />\n!
    return str
  end
end

class SourceFile
  attr_reader :path, :keyPath, :fileId, :fileName, :dir, :comp
  def initialize(path, dir)
    @path = path
    @dir = dir
    @fileId = RootDir.getFileID
    @fileName = File.basename(@path)
    @keyPath = "yes"
    @comp = Component.new(@dir, self)
  end

  def wxs_str(indentCount)
    str = INDENT_STR * indentCount + %Q!<File Id='#{@fileId}' Name='#{@fileName}' KeyPath='#{@keyPath}' DiskId='1' Source='#{RootDir.instance.rootPath}/#{@path}' />\n!
    return str
  end
end

class Component
  attr_reader :compId, :guid, :dir, :file
  attr_writer :withKeyPath
  
  def initialize(dir, file)
    @compId = RootDir.getComponentID
    @guid = RootDir.newGuid
    @dir = dir
    @file = file
    @withKeyPath = false
  end
  
  def filePath
    @file.path
  end

  def wxs_str(indentCount)
    str = ""
    ic = indentCount
    str += INDENT_STR * ic + %Q!<DirectoryRef Id='#{@dir.dirId}'>\n!
    ic += 1
    str += INDENT_STR * ic + %Q!<Component Id='#{@compId}' Guid='#{@guid}'#{@withKeyPath ? " KeyPath='yes'" : ""}>\n!

    str += file.wxs_str(ic + 1)

    str += INDENT_STR * ic + "</Component>\n"
    ic -= 1
    str += INDENT_STR * ic + "</DirectoryRef>\n"
    return str
  end
end

class Feature
  attr_reader :featureId, :title, :description, :paths, :comps
  def initialize(title, description, paths)
    @featureId = RootDir.getFeatureId
    @title = title
    @description = description
    @paths = paths
    @comps = []
  end

  def addComp(comp)
    if containPath(comp.filePath)
      @comps.push(comp)
      return true
    end
    return false
  end
  
  def containPath(tgt)
    for path in @paths
      p = File.expand_path(path)
      t = File.expand_path(tgt)
      if t =~ /^#{p}/
        return true
      end
    end
    return false
  end
  
  def wxs_str(indentCount)
    str = ""
    str += INDENT_STR * indentCount + %Q!<Feature Id='#{@featureId}' Title='#{@title}' Description='#{@description}' TypicalDefault='install' AllowAdvertise='no' Level='1'>\n!
    for comp in @comps
      str += INDENT_STR * (indentCount + 1) + %Q!<ComponentRef Id='#{comp.compId}' />\n!
    end
    str += INDENT_STR * indentCount + "</Feature>\n"
  end
end

def create_wxs(root, template_file, out_wxs_file)
  template = open(template_file).read
  replaced = template.gsub(/<!-- %%(\w+)%% -->/) { |matched|
    case $1
    when "Directory"
      root.wxs_dir_str
    when "Component"
      root.wxs_component_str
    when "Feature"
      root.wxs_feature_str
    else
      $1
    end
  }


  open(out_wxs_file, "w") { |wxs|
    wxs.write replaced
  }
end

CONFIG_YAML_FILE = 'config.yaml'
$config = YAML.load(open(CONFIG_YAML_FILE).read)

root = RootDir.instance

collectData = YAML.load(open($config['COLLECT_YAML_FILE']).read)
for category in collectData
  if category['feature']
    title = category['name']
    description = category['description']
    paths = []
    category['files'].each_pair{|dir, files|
      for file in files
        path = File.join(dir, File.basename(file))
        paths.push(path)
      end
    }
    root.addFeature(title, description, paths)
  end
end

root.init("INSTALLLOCATION", $config['INSTALL_DIR_NAME'], $config['HARVEST_PATH'])
root.addEnvironment("path", "[INSTALLLOCATION]\lib", "set", "last", "yes")

create_wxs(root, $config['TEMPLATE_FILE_E'], $config['OUT_WXS_FILE_E'])
