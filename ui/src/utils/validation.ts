import type { TenantStatus } from '@/types/tenant'
import type { UserStatus } from '@/types/user'

export interface LoginFormState {
  username: string
  password: string
}

export interface RegisterFormState {
  username: string
  nickname: string
  phone: string
  email: string
  password: string
  confirmPassword: string
}

export interface UserCreateFormState extends RegisterFormState {
  status: UserStatus
  tenantId: string
}

export interface UserUpdateFormState {
  nickname: string
  phone: string
  email: string
  status: UserStatus
  tenantId: string
}

export interface TenantFormState {
  tenantCode: string
  tenantName: string
  contactName: string
  contactPhone: string
  description: string
  status: TenantStatus
}

export type FormErrors<T extends string> = Partial<Record<T, string>>

const phonePattern = /^$|^1\d{10}$/
const emailPattern = /^$|^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/

function trimText(value: string) {
  return value.trim()
}

export function normalizeOptionalText(value: string) {
  const normalized = trimText(value)
  return normalized ? normalized : null
}

export function parseTenantIdInput(value: string) {
  const normalized = trimText(value)
  if (!normalized) {
    return null
  }

  const tenantId = Number(normalized)
  return Number.isSafeInteger(tenantId) && tenantId > 0 ? tenantId : null
}

function validateTenantId(value: string) {
  const normalized = trimText(value)
  if (!normalized) {
    return ''
  }

  const tenantId = Number(normalized)
  if (!Number.isSafeInteger(tenantId) || tenantId <= 0) {
    return '租户选择无效，请重新选择。'
  }

  return ''
}

export function validateLoginForm(form: LoginFormState) {
  const errors: FormErrors<keyof LoginFormState> = {}

  if (!trimText(form.username)) {
    errors.username = '请输入用户名。'
  } else if (trimText(form.username).length > 64) {
    errors.username = '用户名长度不能超过 64 位。'
  }

  if (!trimText(form.password)) {
    errors.password = '请输入密码。'
  } else if (trimText(form.password).length < 8 || trimText(form.password).length > 64) {
    errors.password = '密码长度需要在 8 到 64 位之间。'
  }

  return errors
}

export function validateRegisterForm(form: RegisterFormState) {
  const errors: FormErrors<keyof RegisterFormState> = {}

  const username = trimText(form.username)
  const nickname = trimText(form.nickname)
  const phone = trimText(form.phone)
  const email = trimText(form.email)
  const password = trimText(form.password)
  const confirmPassword = trimText(form.confirmPassword)

  if (!username) {
    errors.username = '请输入用户名。'
  } else if (username.length < 4 || username.length > 64) {
    errors.username = '用户名长度需要在 4 到 64 位之间。'
  }

  if (nickname.length > 64) {
    errors.nickname = '昵称长度不能超过 64 位。'
  }

  if (!phonePattern.test(phone)) {
    errors.phone = '手机号格式不正确。'
  }

  if (email.length > 128) {
    errors.email = '邮箱长度不能超过 128 位。'
  } else if (!emailPattern.test(email)) {
    errors.email = '邮箱格式不正确。'
  }

  if (!password) {
    errors.password = '请输入密码。'
  } else if (password.length < 8 || password.length > 64) {
    errors.password = '密码长度需要在 8 到 64 位之间。'
  }

  if (!confirmPassword) {
    errors.confirmPassword = '请再次输入密码。'
  } else if (confirmPassword.length < 8 || confirmPassword.length > 64) {
    errors.confirmPassword = '确认密码长度需要在 8 到 64 位之间。'
  } else if (password && confirmPassword !== password) {
    errors.confirmPassword = '两次输入的密码不一致。'
  }

  return errors
}

export function validateCreateUserForm(form: UserCreateFormState) {
  const errors: FormErrors<keyof UserCreateFormState> = {
    ...validateRegisterForm(form),
  }

  const tenantIdError = validateTenantId(form.tenantId)
  if (tenantIdError) {
    errors.tenantId = tenantIdError
  }

  return errors
}

export function validateUpdateUserForm(form: UserUpdateFormState) {
  const errors: FormErrors<keyof UserUpdateFormState> = {}

  const nickname = trimText(form.nickname)
  const phone = trimText(form.phone)
  const email = trimText(form.email)

  if (nickname.length > 64) {
    errors.nickname = '昵称长度不能超过 64 位。'
  }

  if (!phonePattern.test(phone)) {
    errors.phone = '手机号格式不正确。'
  }

  if (email.length > 128) {
    errors.email = '邮箱长度不能超过 128 位。'
  } else if (!emailPattern.test(email)) {
    errors.email = '邮箱格式不正确。'
  }

  const tenantIdError = validateTenantId(form.tenantId)
  if (tenantIdError) {
    errors.tenantId = tenantIdError
  }

  return errors
}

export function validateTenantForm(form: TenantFormState) {
  const errors: FormErrors<keyof TenantFormState> = {}

  const tenantCode = trimText(form.tenantCode)
  const tenantName = trimText(form.tenantName)
  const contactName = trimText(form.contactName)
  const contactPhone = trimText(form.contactPhone)
  const description = trimText(form.description)

  if (!tenantCode) {
    errors.tenantCode = '请输入租户编码。'
  } else if (tenantCode.length > 64) {
    errors.tenantCode = '租户编码长度不能超过 64 位。'
  }

  if (!tenantName) {
    errors.tenantName = '请输入租户名称。'
  } else if (tenantName.length > 128) {
    errors.tenantName = '租户名称长度不能超过 128 位。'
  }

  if (contactName.length > 64) {
    errors.contactName = '联系人长度不能超过 64 位。'
  }

  if (!phonePattern.test(contactPhone)) {
    errors.contactPhone = '联系电话格式不正确。'
  }

  if (description.length > 500) {
    errors.description = '租户描述长度不能超过 500 位。'
  }

  return errors
}
