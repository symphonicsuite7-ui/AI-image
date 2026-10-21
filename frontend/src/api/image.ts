import http from './http'
import type {
  ApiResponse,
  EditImageUploadRequest,
  EditImageUrlRequest,
  GenerateImageRequest,
  GenerateImageResult,
  ImageHistory,
  PageResult,
} from '@/types/image'
import { resolveImageUrl } from '@/utils/url'

export async function generateImage(payload: GenerateImageRequest) {
  const { data } = await http.post<ApiResponse<GenerateImageResult>>('/api/image/generate', payload)
  return { ...data.data, imageUrl: resolveImageUrl(data.data.imageUrl) }
}

export async function editImage(payload: EditImageUploadRequest) {
  const formData = new FormData()
  if (payload.stylePrompt.trim()) formData.append('stylePrompt', payload.stylePrompt.trim())
  formData.append('prompt', payload.prompt)
  formData.append('size', payload.size)
  formData.append('quality', payload.quality)
  formData.append('image', payload.image)
  if (payload.mask) formData.append('mask', payload.mask)

  const { data } = await http.post<ApiResponse<GenerateImageResult>>('/api/image/edit', formData)
  return { ...data.data, imageUrl: resolveImageUrl(data.data.imageUrl) }
}

export async function editImageByUrl(payload: EditImageUrlRequest) {
  const { data } = await http.post<ApiResponse<GenerateImageResult>>('/api/image/edit-by-url', payload)
  return { ...data.data, imageUrl: resolveImageUrl(data.data.imageUrl) }
}

export async function getImageHistory(page = 1, size = 12) {
  const { data } = await http.get<ApiResponse<PageResult<ImageHistory>>>('/api/image/history', {
    params: { page, size },
  })
  return {
    ...data.data,
    records: data.data.records.map((item) => ({
      ...item,
      imageUrl: resolveImageUrl(item.imageUrl),
    })),
  }
}

export async function deleteImage(id: number) {
  await http.delete<ApiResponse<null>>(`/api/image/${id}`)
}
