package myexpressionfriend_api.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import myexpressionfriend_api.common.domain.PeersTheme;

@Converter
public class PeersThemeConverter implements AttributeConverter<PeersTheme, String> {

    @Override
    public String convertToDatabaseColumn(PeersTheme attribute) {
        return attribute == null ? null : attribute.getDisplayName();
    }

    @Override
    public PeersTheme convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PeersTheme.fromDisplayName(dbData);
    }
}
