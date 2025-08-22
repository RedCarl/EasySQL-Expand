package ${dao.packageName};

import cn.carljoy.easysql.BaseDao;
import <#if domain.basePackage??>${domain.basePackage}<#else>${domain.packageName?replace(".dao", "")}</#if>.${tableClass.shortClassName};

import java.sql.SQLException;
<#list tableClass.importList as fieldType>
<#if fieldType?contains("java.sql") || fieldType?contains("java.util.Date") || fieldType?contains("java.time")>
import ${fieldType};
</#if>
</#list>

/**
 * ${tableClass.remark!} DAO
 * 
 * @author <#if author??>${author}<#else>Generated</#if>
 * @since <#if date??>${date}<#else>${.now?string("yyyy-MM-dd")}</#if>
 */
public class ${tableClass.shortClassName}Dao extends BaseDao<${tableClass.shortClassName}> {

    public ${tableClass.shortClassName}Dao(cc.carm.lib.easysql.api.SQLManager sqlManager) {
        super(${tableClass.shortClassName}.class, sqlManager);
    }

<#-- 为唯一字段或常用查询字段生成简单的查询方法 -->
<#list tableClass.baseFields as field>
    <#if !(field.primaryKey?? && field.primaryKey) && ((field.unique?? && field.unique) || field.columnName == "username" || field.columnName == "email" || field.fieldName == "username" || field.fieldName == "email")>
    /**
     * 根据 ${field.remark!field.fieldName} 查询
     */
    public ${tableClass.shortClassName} findBy${field.fieldName?cap_first}(${field.shortTypeName} ${field.fieldName}) throws SQLException {
        return selectOneByQuery(createQuery().eq("${field.fieldName}", ${field.fieldName}));
    }

    </#if>
</#list>
    // 可以在这里添加更多自定义查询方法
}