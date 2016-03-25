<?xml version="1.0" ?>

<xsl:stylesheet  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"   version="1.0">

<xsl:param name="h-style" select="'color: red; font-family: arial;'" />
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
         <h2 style="{$h-style}">Normative State: <i><xsl:value-of select="@id" /></i></h2>

        <table border="0" cellspacing="3" cellpadding="6">
        <tr style="{$trh-style}"> 
        <th valign="top" style="{$th-style}">state</th>
        <th valign="top" style="{$th-style}">agent</th>
        <th valign="top" style="{$th-style}">reason (norm)</th> 
        <th valign="top" style="{$th-style}">goal</th>
        <th valign="top" style="{$th-style}">time to fulfil</th>
        <th valign="top" style="{$th-style}">done at</th>
        <th valign="top" style="{$th-style}">annotations</th>
        </tr>
        <xsl:apply-templates select="obligation[@state='active']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="obligation[@state='unfulfilled']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="obligation[@state='fulfilled']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        <xsl:apply-templates select="obligation[@state='inactive']" >
             <xsl:sort select="@agent" />
        </xsl:apply-templates>
        </table>
      </body>
   </html>
</xsl:template>


<xsl:template match="obligation">
    <tr style="{$trh-style}">
    
    <td style="{$td-style}">
        <xsl:if test="position()=1">
            <b><xsl:value-of select="@state" /></b>
        </xsl:if>
    </td>
    <td style="{$td-style}"><xsl:value-of select="@agent" /></td>
    <td style="{$td-style}"><xsl:value-of select="@reason" /></td>
    <td style="{$td-style}"><xsl:value-of select="@object" /></td>
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