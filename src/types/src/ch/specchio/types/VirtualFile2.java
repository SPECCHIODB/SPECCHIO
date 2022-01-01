package ch.specchio.types;

import java.io.File;
import java.io.IOException;


public class VirtualFile2 extends File {
	
    private static final long serialVersionUID = -1752685357864733168L;
    boolean is_dir;

    VirtualFile2(final File file) {
        super(file.toString());
    }
    
    VirtualFile2(final File file, boolean is_dir) {
        super(file.toString());
        this.is_dir = is_dir;
    }    

    VirtualFile2(String pathname) {
        super(pathname);
    }
    
    VirtualFile2(String pathname, boolean is_dir) {
        super(pathname);
        this.is_dir = is_dir;
    }    

    private VirtualFile2(String parent, String child) {
        super(parent, child);
    }

    VirtualFile2(File parent, String child) {
        super(parent, child);
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return is_dir;
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return new VirtualFile2(super.getCanonicalFile());
    }

    @Override
    public File getAbsoluteFile() {
        return new VirtualFile2(super.getAbsoluteFile());
    }

    @Override
    public File getParentFile() {
        File parent = super.getParentFile();

        if (parent != null) {
            parent = new VirtualFile2(super.getParentFile());
        }
        return parent;
    }
    
    @Override
    public DropboxPath toPath() {
    	
    	return new  DropboxPath(this.getPath());
    }


}
