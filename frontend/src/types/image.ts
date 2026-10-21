export type ImageQuality = 'low' | 'medium' | 'high'
export type ImageMode = 'generate' | 'edit'
export type EditSourceMode = 'upload' | 'url'
export type ImageSize =
  | '1024x1024'
  | '1024x1536'
  | '1536x1024'
  | '1152x2048'
  | '2048x1152'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface GenerateImageRequest {
  stylePrompt: string
  prompt: string
  size: ImageSize
  quality: ImageQuality
}

export interface GenerateImageResult {
  id: number
  imageUrl: string
  elapsedMillis: number
}

export interface EditImageUploadRequest extends GenerateImageRequest {
  image: File
  mask?: File
}

export interface EditImageUrlRequest extends GenerateImageRequest {
  imageUrl: string
}

export interface ImageHistory {
  id: number
  prompt: string
  imageUrl: string
  model: string
  size: string
  quality: ImageQuality
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
