package pl.edu.agh.backend.material;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException() {
        super("Material not found");
    }
}
