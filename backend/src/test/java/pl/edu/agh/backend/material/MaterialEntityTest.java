package pl.edu.agh.backend.material;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaterialEntityTest {

    @Test
    void storesCoreFieldsAndOptionalEventLink() {
        Material material = new Material();
        material.setTitle("Nagranie wykładu");
        material.setDescription("Opis");
        material.setType(MaterialType.RECORDING);
        material.setUrl("https://example.com/recording");

        assertThat(material.getTitle()).isEqualTo("Nagranie wykładu");
        assertThat(material.getType()).isEqualTo(MaterialType.RECORDING);
        assertThat(material.getEvent()).isNull();
    }
}
