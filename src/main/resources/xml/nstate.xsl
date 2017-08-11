<?xml version="1.0" ?>

<xsl:stylesheet  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"   version="1.0">

<xsl:param name="h-style" select="'color: red; font-family: arial; font-weight: normal'" />
<xsl:param name="h3-style" select="'color: green; font-family: arial;'" />
<xsl:param name="txt-style" select="'font-family: arial;'" />

<xsl:param name="th-style" select="'text-align: left; vertical-align: top;  color: #330099;'" />
<xsl:param name="td-style"  select="'text-align: left; vertical-align: top;'" />
<xsl:param name="trh-style" select="'background-color: #ece7e6; font-family: arial; vertical-align: top;'" />
<xsl:param name="tr-style"  select="'background-color: #ece7e6; font-family: arial;'" />

<xsl:output encoding="ISO-8859-1" method="html" />
<xsl:strip-space elements="*"/>

<xsl:template match="normative-state">
   <html>
      <body>
         <h2 style="{$h-style}">Normative State</h2>

        <table border="0" cellspacing="3" cellpadding="6">
        <tr style="{$trh-style}">
        <th valign="top" style="{$th-style}">state</th>
        <th valign="top" style="{$th-style}">agent</th>
        <th valign="top" style="{$th-style}">maintenance condition</th>
        <th valign="top" style="{$th-style}">aim</th>
        <th valign="top" style="{$th-style}">deadline</th>
        <th valign="top" style="{$th-style}">done at</th>
        <th valign="top" style="{$th-style}">annotations</th>
        </tr>
        <xsl:apply-templates select="deontic-modality[@state='active']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="deontic-modality[@state='unfulfilled']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="deontic-modality[@state='fulfilled']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="deontic-modality[@state='inactive']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        </table>
      </body>
   </html>
</xsl:template>


<xsl:template match="deontic-modality">
    <tr style="{$trh-style}">

    <td style="{$td-style}">
        <xsl:if test="position()=1">
            <b><xsl:value-of select="@state" /></b>
        </xsl:if>
    </td>
    <td style="{$td-style}"><xsl:value-of select="@agent" /></td>
    <td style="{$td-style}"><xsl:value-of select="@maintenance" /></td>
    <td style="{$td-style}">
        <xsl:if test="@modality = 'obligation'">
            <b>O </b>
        </xsl:if>
        <xsl:if test="@modality = 'permission'">
            <b>P </b>
        </xsl:if>
        <xsl:if test="@modality = 'prohibition'">
            <b>F </b>
        </xsl:if>
        <xsl:value-of select="@aim" />
    </td>
    <td style="{$td-style}"><xsl:value-of select="@ttf" /></td>
    <td style="{$td-style}"><xsl:value-of select="@done" /></td>
    <td style="{$td-style}"><xsl:apply-templates /></td>
    </tr>
</xsl:template>

<xsl:template match="annotation">
    <xsl:value-of select="@id" /> =
    <xsl:value-of select="@value" />
    <br/>
</xsl:template>

</xsl:stylesheet>