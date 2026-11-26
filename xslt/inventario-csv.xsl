<?xml version="1.0" encoding="UTF-8"?>
<!-- XML -> CSV (texto plano, separador «;»). Limitación: si un dato contuviera «;» habría que entrecomillarlo. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/inventario">
        <xsl:text>id;nombre;categoria;precio;stock&#10;</xsl:text>
        <xsl:for-each select="producto">
            <xsl:value-of select="@id"/><xsl:text>;</xsl:text>
            <xsl:value-of select="normalize-space(nombre)"/><xsl:text>;</xsl:text>
            <xsl:value-of select="normalize-space(categoria)"/><xsl:text>;</xsl:text>
            <xsl:value-of select="precio"/><xsl:text>;</xsl:text>
            <xsl:value-of select="stock"/><xsl:text>&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
