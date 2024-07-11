package com.ec.web.system;

import com.ec.common.annotation.Log;
import com.ec.common.core.controller.BaseController;
import com.ec.common.core.domain.AjaxResult;
import com.ec.common.core.page.TableDataInfo;
import com.ec.common.enums.BusinessType;
import com.ec.saas.domain.MasterTenant;
import com.ec.saas.service.IMasterTenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 囍崽
 * version 1.0
 * <p>
 * 租户信息操作处理
 */

/**
 * <h3>RestFull风格</h3>
 * 四种提交方式代表增删改查<br/>
 *
 * @ GetMapping: 组合注解，通常用来处理get请求，常用于执行查询操作<br/>
 * @ PostMapping: 组合注解，通常用来处理post请求，常用于执行添加操作<br/>
 * @ PutMapping: 组合注解，通常用来处理put请求，用于更新操作<br/>
 * @ DeleteMapping: 组合注解，用来处理delete请求，常用于执行删除操作<br/>
 * @ RequestMapping； 用于处理所有类型的HTTP请求<br/>
 *
 * <h3>自定义权限实现</h3>
 * <p>
 * @ PreAuthorize("@ss.hasPermi('system:tenant:edit')")的意思<br>
 * 1.ss是一个注册在Spring容器中的bean，对应的类是cn.hadoopx.framework.web.service.PermissionService；<br/>
 * 2.hasPermi是PermissionService类中定义的方法<br>
 * 3.当Spring EL表达式返回TRUE，则权限校验通过.<br>
 * <strong>扩展：</strong>:Spring EL格式为#{SpEL expression}
 * </p>
 */
@RestController
@RequestMapping("/system/tenant")
public class SysTenantController extends BaseController {

    @Autowired
    private IMasterTenantService masterTenantService;

    /**
     * 获取租户列表
     *
     * @param masterTenant
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:list')")
    @GetMapping("/list")
    public TableDataInfo list(MasterTenant masterTenant) {
        List<MasterTenant> masterTenants = masterTenantService.selectMasterTenants(masterTenant);
        return getDataTable(masterTenants);
    }

    /**
     * 根据id获取详细信息
     *
     * @param id
     * @return
     */
    @PreAuthorize("@sshasPermi('system:tenant:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(masterTenantService.selectMasterTenantById(id));
    }

    /**
     * 修改租户
     *
     * @param masterTenant
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MasterTenant masterTenant) {
        return toAjax(masterTenantService.updateMasterTenant(masterTenant));
    }

    /**
     * 删除用户
     *
     * @param ids
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:remove')")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(masterTenantService.deleteMasterTenantByIds(ids));
    }

}
