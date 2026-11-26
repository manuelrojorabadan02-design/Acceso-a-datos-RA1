<?xml version="1.0" encoding="UTF-8"?>
<!-- XML -> HTML (XSLT 1.0, el que incluye el JDK) -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/inventario">
        <html>
            <head>
                <title>Inventario</title>
                <style>
                    body { font-family: sans-serif; margin: 2em; }
                    table { border-collapse: collapse; }
                    th, td { border: 1px solid #999; padding: 4px 10px; text-align: left; }
                    th { background: #1F3864; color: white; }
                    .agotado { color: #b00020; font-weight: bold; }
                </style>
            </head>
            <body>
                <h1>Inventario a fecha <xsl:value-of select="@fecha"/></h1>
                <p><xsl:value-of select="count(producto)"/> productos.</p>
                <table>
                    <tr><th>Id</th><th>Producto</th><th>Categoría</th><th>Precio</th><th>Stock</th></tr>
                    <xsl:for-each select="producto">
                        <xsl:sort select="nombre"/>
                        <tr>
                            <td><xsl:value-of select="@id"/></td>
                            <td><xsl:value-of select="nombre"/></td>
                            <td><xsl:value-of select="categoria"/></td>
                            <td><xsl:value-of select="format-number(precio, '0.00')"/><xsl:text> </xsl:text><xsl:value-of select="precio/@moneda"/></td>
                            <td>
                                <xsl:if test="stock = 0">
                                    <xsl:attribute name="class">agotado</xsl:attribute>
                                </xsl:if>
                                <xsl:value-of select="stock"/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
