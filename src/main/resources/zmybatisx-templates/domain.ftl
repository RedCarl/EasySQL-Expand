package ${domain.packageName};

import cn.carljoy.easysql.annotation.Column;
import cn.carljoy.easysql.annotation.Id;
import cn.carljoy.easysql.annotation.Table;
import cn.carljoy.easysql.annotation.CreatedAt;
import cn.carljoy.easysql.annotation.UpdatedAt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

<#list tableClass.importList as fieldType>
import ${fieldType};
</#list>

/**
 * ${tableClass.remark!}
 * 演示如何使用注解来定义表结构
 * 
 * @author <#if author??>${author}<#else>Generated</#if>
 * @since <#if date??>${date}<#else>${.now?string("yyyy-MM-dd")}</#if>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(value = "${tableClass.tableName}"<#if tableClass.remark??>, comment = "${tableClass.remark}"</#if>)
public class ${tableClass.shortClassName} {

<#list tableClass.allFields as field>
    <#if field.fieldName=="id">
    @Id
    <#elseif field.columnName == "created_at" || field.columnName == "create_time" || field.fieldName == "createdAt">
    @CreatedAt
    @Column(name = "${field.columnName}"<#if field.columnType??>, type = "${field.columnType}"</#if><#if field.nullable?? && !field.nullable>, nullable = false</#if><#if field.remark??>, comment = "${field.remark}"</#if>)
    <#elseif field.columnName == "updated_at" || field.columnName == "update_time" || field.fieldName == "updatedAt">
    @UpdatedAt
    @Column(name = "${field.columnName}"<#if field.columnType??>, type = "${field.columnType}"</#if><#if field.nullable?? && !field.nullable>, nullable = false</#if><#if field.remark??>, comment = "${field.remark}"</#if>)
    <#else>
    @Column(name = "${field.columnName}"<#if field.columnType??>, type = "${field.columnType}"</#if><#if field.nullable?? && !field.nullable>, nullable = false</#if><#if (field.unique?? && field.unique)>, unique = true</#if><#if field.defaultValue??>, defaultValue = "${field.defaultValue}"</#if><#if field.remark??>, comment = "${field.remark}"</#if>)
    </#if>
    public <#if field.columnType?? && (field.columnType?upper_case == "FLOAT" || field.columnType?upper_case == "REAL")>Float<#elseif field.columnType?? && (field.columnType?upper_case == "DOUBLE" || field.columnType?upper_case == "DOUBLE PRECISION")>Double<#else>${field.shortTypeName}</#if> ${field.fieldName};

</#list>
}