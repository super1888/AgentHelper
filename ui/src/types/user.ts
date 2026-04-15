import type { PageInfoResponse } from '@/types/api'

export type UserStatus = 0 | 1

export interface UserProfile {
  id: number
  username: string
  nickname: string
  phone: string | null
  email: string | null
  status: UserStatus
  tenantId: number | null
}

export interface UserToken {
  tokenName: string
  tokenPrefix: string | null
  tokenValue: string
  authorizationValue: string
  expiresIn: number
  loginId: number
}

export interface UserAuthLoginResult {
  user: UserProfile
  token: UserToken
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  nickname: string | null
  phone: string | null
  email: string | null
  password: string
  confirmPassword: string
}

export interface CreateUserPayload extends RegisterPayload {
  status: UserStatus
  tenantId: number | null
}

export interface UpdateUserPayload {
  nickname: string | null
  phone: string | null
  email: string | null
  status: UserStatus
  tenantId: number | null
}

export interface UserQueryPayload {
  pageNum?: number
  pageSize?: number
  username?: string
  nickname?: string
  phone?: string
  email?: string
  status?: UserStatus | null
}

export interface UserStatistics {
  totalCount: number
  enabledCount: number
  disabledCount: number
  tenantCount: number
}

export type UserPageResult = PageInfoResponse<UserProfile>
