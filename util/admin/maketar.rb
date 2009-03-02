
require "fileutils"

def maketar(target_top_dir, files, archive_top_dir, cut_top_folder, use_zip = false)

  if FileTest.exist?(archive_top_dir)
    print "overwrite " + archive_top_dir + " ? (y/n) "
    answer = STDIN.gets
    if answer.chomp == "y"
      FileUtils.rm_r archive_top_dir
    else
      exit
    end
  end

  for file in files
    src  = target_top_dir  + "/" + file

    unless FileTest.exist?(src)
      puts src + " does not exist" 
      next 
    end

    dest = archive_top_dir + "/" + file

    dest_dir = FileTest.directory?(src) ? dest : File.dirname(dest)
    FileUtils.mkdir_p(dest_dir) unless FileTest.exist?(dest_dir)
    FileUtils.cp(src, dest_dir) if FileTest.file?(src)
  end

  if use_zip
    zipfile = archive_top_dir + ".zip"
    File.unlink(zipfile) if File.exist?(zipfile)
    command = "zip -r #{zipfile} #{archive_top_dir}"
    puts command
    system(command)
  else 
    if !cut_top_folder
      system("tar -czf #{archive_top_dir}.tar.gz #{archive_top_dir}")
    else 
      Dir.chdir(archive_top_dir){
        command = "tar -czf ../#{archive_top_dir}.tar.gz *"
        puts command
        system(command)
      }
    end
  end

  FileUtils.rm_r archive_top_dir if FileTest.exist?(archive_top_dir)

end

