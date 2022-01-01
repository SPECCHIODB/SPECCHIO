package ch.specchio.file.reader.campaign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import com.dropbox.core.DbxException;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import ch.specchio.types.DropboxPath;

public class VirtualFileSystemView extends FileSystemView {

    final Path base;
    DbxClientV2 client;
    final Set<DropboxPath> choices;

    public VirtualFileSystemView(final Path base, DbxClientV2 client) {
        this.base = base;
        this.client = client;
        this.choices = new TreeSet<DropboxPath>();
        
//        ListFolderResult initial_contents;
//		try {
//			initial_contents = client.files().listFolder("");
//			
//	        for (Metadata entry : initial_contents.getEntries())
//	        {
//	        	// only show folders
//	        	if(entry.getClass() == FolderMetadata.class)
//	        	{
//	        		DropboxPath new_entry = new DropboxPath(entry.getPathLower(), true);
//	        		choices.add(new_entry);
//	        		
//	        	}    	        	
//	        	
//	        }			
//		} catch (ListFolderErrorException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (DbxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        

    }

    @Override
    protected File createFileSystemRoot(File f) {
        return new VirtualFile(f);
    }

    @Override
    public boolean isComputerNode(File dir) {
        return false;
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    @Override
    public boolean isDrive(File dir) {
        return false;
    }

    @Override
    public Icon getSystemIcon(File f) {
        return null;
    }

    @Override
    public String getSystemTypeDescription(File f) {
        return f.toPath().toString();
    }

    @Override
    public String getSystemDisplayName(File f) {
        return f.getName();
    }

    @Override
    public File getParentDirectory(File dir) {

    	if (dir != null)
    	{

    		File parent = dir.getParentFile();

    		if(parent != null)
    			return new VirtualFile(parent, true);
    	}

    	return null;
    }

    @Override
    
    // use synchronized here: but why the devil are there multiple threads reading the same information about 6 times?!?!?!?
    
    public synchronized File[] getFiles(final File dir, boolean useFileHiding) {
//        final List<File> files = new ArrayList<>(choices.size());
//        
//        
//        Stream<DropboxPath> s = choices.stream();
    	
    	List<DropboxPath> folders = new ArrayList<>();
    	List<File> files = new ArrayList<>();
    	ListFolderResult contents;
		try {
			
			if(isFileSystemRoot(dir))
			{
				contents = client.files().listFolder("");
			}
			else
			{
				String p = dir.toString();
				contents = client.files().listFolder(p);
			}
			
			
			
			
	        for (Metadata entry : contents.getEntries())
	        {
	        	// only show folders
	        	if(entry.getClass() == FolderMetadata.class)
	        	{
	        		DropboxPath new_entry = new DropboxPath(entry.getPathLower(), true);
	        		folders.add(new_entry); 
	        		choices.add(new_entry);
	        		
	        		files.add(new VirtualFile(new_entry.toFile(), true));
	        	}    	
	        	else // other files
	        	{

	        		
	        		File f = createFileObject(dir, entry.getName());
	        		
	        		files.add(new VirtualFile(f));
	        	}
	        	
	        }    	
	        
	        
		} catch (ListFolderErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		

        
//        Iterator<DropboxPath> i = s.iterator();
//        
//        while(i.hasNext())
//        {
//        	Path p = i.next();
//        	
//        	Path parent = p.getParent();
//        	Path dir_path = dir.toPath();
//        	
//        	boolean parent_match = parent.toString().equals(dir_path.toString());
//        	
//        	if(parent_match)
//        	{
//        		files.add(new VirtualFile(p.toFile(), true));
//        	}
        	
//        }

//        choices.stream()
//                    .filter((path) -> (path.getParent().equals(dir.toPath()))).
//                    forEach((path) -> {
//                        files.add(new VirtualFile(path.toFile()));
//                    });
        
        File[] arr = files.toArray(new File[files.size()]);

        return files.toArray(new File[files.size()]);
    }

    @Override
    public File createFileObject(String path) {
    	File f = new VirtualFile(path, true);
    	boolean dir = f.isDirectory();
        return f;
    }

    @Override
    public File createFileObject(final File dir, final String filename) {
        Path fileObject;

        if (dir != null) {
            fileObject = Paths.get(dir.toPath().toString(), filename);
        } else {
            fileObject = Paths.get(filename);
        }
        return new VirtualFile(fileObject.toFile());
    }

    @Override
    public File getDefaultDirectory() {
    	File f = new VirtualFile(base.toFile(), true);
        return f;
    }

    @Override
    public File getHomeDirectory() {
        return new VirtualFile(base.toFile());
    }

    @Override
    public File[] getRoots() {
        final List<File> files = new ArrayList<>();

        files.add(new VirtualFile(base.toFile(), true));
        return files.toArray(new File[files.size()]);
    }

    @Override
    public boolean isFileSystemRoot(final File dir) {
    	Path parent = dir.toPath().getParent();
        boolean isRoot = parent.toString() == null;
        return isRoot;
    }

    @Override
    public boolean isHiddenFile(final File f) {
        return false;
    }

    @Override
    public boolean isFileSystem(final File f) {
        return !isFileSystemRoot(f);
    }

    @Override
    public File getChild(final File parent, final String fileName) {
        return new VirtualFile(parent, fileName);
    }

    @Override
    public boolean isParent(final File folder, final File file) {
    	
    	Path parent = file.toPath().getParent();
    	Path folder_path = folder.toPath();
    	
    	boolean ck = parent.toString().equals(folder_path.toString());
    	
    	//boolean ck = file.toPath().getParent().equals(folder.toPath());
        return ck;
    }

//    @Override
//    public Boolean isTraversable(File f) {
//        boolean isTraversable = false;
//
////        for (final Path path : choices) {
//////        	Path ck = f.toPath();
//////        	
//////        	boolean b = path.startsWith(ck);
////        	
//////        	test_path = 
////        	
////            if (path.startsWith(f.toPath())) {
////                isTraversable = true;
////                break;
////            }
////        }
//       return isTraversable;
//        
////        return true;
//    }

    @Override
    public boolean isRoot(final File f) {
        boolean isRoot = false;

        for (final Path path : choices) {
            if (path.getParent().equals(f.toPath())) {
                isRoot = true;
            }
        }
        return isRoot;
    }

    @Override
    public File createNewFolder(final File containingDir) throws IOException {
        return new VirtualFile(containingDir);
    }


    private class VirtualFile extends File {

        private static final long serialVersionUID = -1752685357864733168L;
        boolean is_dir;

        private VirtualFile(final File file) {
            super(file.toString());
        }
        
        public VirtualFile(File file, boolean is_dir) {
        	super(file.toString());
        	this.is_dir = is_dir;
		}  

        private VirtualFile(String pathname) {
            super(pathname);
        }
        
        VirtualFile(String pathname, boolean is_dir) {
            super(pathname);
            this.is_dir = is_dir;
        }            

        private VirtualFile(String parent, String child) {
            super(parent, child);
        }

        private VirtualFile(File parent, String child) {
            super(parent, child);
        }


		@Override
        public boolean exists() {
            return true;
        }
		
        @Override
        public boolean isFile() {
        	return !this.is_dir;
//            return VirtualFileSystemView.this.isTraversable(this);
        }		

        @Override
        public boolean isDirectory() {
        	return this.is_dir;
//            return VirtualFileSystemView.this.isTraversable(this);
        }

        @Override
        public File getCanonicalFile() throws IOException {
            return new VirtualFile(super.getCanonicalFile());
        }

        @Override
        public File getAbsoluteFile() {
        	File f = new VirtualFile(super.getAbsoluteFile(), true);
            return f;
        }

        @Override
        public File getParentFile() {
            File parent = super.getParentFile();

            if (parent != null) {
                parent = new VirtualFile(super.getParentFile(), true);
            }
            return parent;
        }
        
        @Override
        public DropboxPath toPath() {
        	
        	return new  DropboxPath(this.getPath(), is_dir);
        }
        
        @Override
        public File[] listFiles() {
        	
        	File[] files = new File[3];
        	
        	return files;
        	
        }

    }

}