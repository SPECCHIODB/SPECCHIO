package ch.specchio.file.reader.campaign;

import java.io.File;
import java.io.IOException;

import ch.specchio.types.DropboxPath;


public class VirtualFile extends File {

    private static final long serialVersionUID = -1752685357864733168L;
    boolean is_dir;

    public VirtualFile(final File file) {
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

    public VirtualFile(File parent, String child) {
        super(parent, child);
    }


	@Override
    public boolean exists() {
        return true;
    }
	
    @Override
    public boolean isFile() {
    	return !this.is_dir;
//        return VirtualFileSystemView.this.isTraversable(this);
    }		

    @Override
    public boolean isDirectory() {
    	return this.is_dir;
//        return VirtualFileSystemView.this.isTraversable(this);
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