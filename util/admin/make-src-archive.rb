#!/usr/bin/ruby

SRC_REPOSITORY_PATH = "https://openrtp.jp/svn/hrg/openhrp/3.1/trunk"
EXPORT_TMP_DIR = "tmp_exported"

require "optparse"
require "maketar"

script_file = nil
command = String("svn export #{SRC_REPOSITORY_PATH} #{EXPORT_TMP_DIR}")
use_zip = false

parser = OptionParser.new

parser.banner = <<END
This is a scirpt for making a source archive from a svn working directory tree

usage: #{File.basename($0)} <archive> [list ...]

 archive: name of the archive top directory (archive.tar.gz is created)
 list:    a file that lists the files to exclude from the source archive.
END

parser.on("--script script", String) { |script|
  script_file = File.expand_path(script)
}

parser.on("--svn-path svnPath", String) { |svnPath|
  command = String( "svn export " + svnPath + " #{EXPORT_TMP_DIR}" )
}

parser.on("--use-zip") { use_zip = true }


begin 
  parser.parse!
rescue OptionParser::ParseError => err
  STDERR.puts err.message
  STDERR.puts parser.help
  exit 1
end

if ARGV.size < 2
  STDERR.puts parser.help
  exit 1
end

end_slash = %r|/$|
archive_top_dir = ARGV[0].sub(end_slash, "")

filelists = ARGV[1..ARGV.size]

exclude_list = [ ]

for filename in filelists
  open(filename) do | f |
    while line = f.gets do
      path = line.chomp
      # Comment out such like C++ '//'
      path = path.sub( /\s*\/\/.*/, '' )
      if path !~ /^\s*$/   # not empty line
        pattern = "^" + Regexp.quote(path)
        re = Regexp.new(pattern.gsub("\\*", "[^/]+").gsub("\\?", "[^/]"))
        exclude_list << re
      end
    end
  end
end

files = [ ]



puts command
command.insert(0,"|")

open(command.to_str) { |io|
  while io.gets
    print "."
    STDOUT.flush
  end
  print "\n"
}


pwd = Dir.pwd
Dir.chdir EXPORT_TMP_DIR

if script_file
  print "execute script " + script_file, "\n"
  puts `#{script_file}`
end


Dir.glob("**/*.*\0**/.*\0**/*").each { |path|
  next unless exclude_list.each { |pattern|
    if path =~ pattern
      print "skip ", path.chomp, "\n"
      break
    end
  }
  files << path.chomp
}


Dir.chdir pwd

maketar(EXPORT_TMP_DIR, files, archive_top_dir, false, use_zip)

FileUtils.rm_r EXPORT_TMP_DIR if FileTest.exist?(EXPORT_TMP_DIR)
