package com.ec.web.system;

import com.ec.auth.web.service.SysLoginService;
import com.ec.auth.web.service.SysRegisterService;
import com.ec.auth.web.service.TenantRegisterService;
import com.ec.common.constant.TenantConstants;
import com.ec.common.core.controller.BaseController;
import com.ec.common.core.domain.AjaxResult;
import com.ec.saas.dto.TenantDatabaseDTO;
import com.ec.saas.form.TenantRegisterBody;
import com.ec.saas.service.IMasterTenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * @author 囍崽
 * version 1.0
 * 注册验证
 */
@RestController
public class SysRegisterController extends BaseController {

    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private IMasterTenantService masterTenantService;

    @Autowired
    private TenantRegisterService tenantRegisterService;

    public AjaxResult registerTenant(@RequestBody TenantRegisterBody tenantRegisterBody){
        loginService.validateCaptcha(tenantRegisterBody.getTenantName(),tenantRegisterBody.getCode(),tenantRegisterBody.getUuid());

        if (TenantConstants.NOT_UNIQUE.equals(masterTenantService.checkTenantNameUnique(tenantRegisterBody.tenantName))){
            return AjaxResult.error("注册'" + tenantRegisterBody.getTenantName() +"'失败，账号已存在");
        }
        TenantDatabaseDTO tenantDatabaseDTO = null;
        try {
            tenantDatabaseDTO = tenantRegisterService.initDatabase(tenantRegisterBody);
        } catch (SQLException e) {
            e.printStackTrace();
            return AjaxResult.error("注册'"+tenantRegisterBody.getTenantName() + "'失败，创建租户时发生错误");
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResult.error("注册'"+tenantRegisterBody.getTenantName()+"'失败，请与我们联系");
        }
        int i = masterTenantService.insertMasterTenant(tenantDatabaseDTO);
        return toAjax(i);
    }
}
